/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.commands.library;

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.commands.SRCommand;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.commands.library.types.PlayerSelectorArgumentParser;
import net.skinsrestorer.shared.config.ProxyConfig;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ComponentHelper;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.brigadier.BrigadierManagerHolder;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.permission.PredicatePermission;
import org.incendo.cloud.processors.cooldown.*;
import org.incendo.cloud.processors.cooldown.listener.CooldownActiveListener;
import org.incendo.cloud.processors.cooldown.listener.ScheduledCleanupCreationListener;
import org.incendo.cloud.processors.cooldown.profile.CooldownProfile;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.annotation.RequirementBindings;
import org.incendo.cloud.services.type.ConsumerService;
import org.incendo.cloud.translations.TranslationBundle;
import org.incendo.cloud.translations.minecraft.extras.MinecraftExtrasTranslationBundle;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public class SRCommandManager {
    public static final CloudKey<String> BUKKIT_DESCRIPTION = CloudKey.of("bukkit_description", String.class);
    @Getter
    private final CommandManager<SRCommandSender> commandManager;
    private final AnnotationParser<SRCommandSender> annotationParser;
    private final CooldownRepository<SRCommandSender> cooldownRepository = CooldownRepository.mapping(
            sender -> {
                if (sender instanceof SRPlayer player) {
                    return player.getUniqueId();
                }

                throw new IllegalArgumentException("Only SRPlayer is supported");
            },
            CooldownRepository.forMap(new HashMap<>())
    );
    private final CooldownManager<SRCommandSender> cooldownManager = CooldownManager.cooldownManager(CooldownConfiguration.<SRCommandSender>builder()
            .repository(cooldownRepository)
            .addCreationListener(new ScheduledCleanupCreationListener<>(Executors.newSingleThreadScheduledExecutor(), cooldownRepository))
            .addAllActiveCooldownListeners(List.of(new CooldownMessenger()))
            .bypassCooldown(context -> !(context.sender() instanceof SRPlayer) || context.sender().hasPermission(PermissionRegistry.BYPASS_COOLDOWN))
            .build());

    @SuppressWarnings("unchecked")
    @Inject
    public SRCommandManager(SRPlatformAdapter platform, SkinsRestorerLocale locale, SettingsManager settingsManager) {
        this.commandManager = platform.createCommandManager();
        this.annotationParser = new AnnotationParser<>(commandManager, SRCommandSender.class);

        if (commandManager instanceof BrigadierManagerHolder<?, ?> holder && holder.hasBrigadierManager()) {
            CloudBrigadierManager<SRCommandSender, ?> brigadierManager = (CloudBrigadierManager<SRCommandSender, ?>) holder.brigadierManager();
            brigadierManager.setNativeNumberSuggestions(true);
            brigadierManager.settings().set(BrigadierSetting.FORCE_EXECUTABLE, true);
        }

        commandManager.captionRegistry().registerProvider(TranslationBundle.core(SRCommandSender::getLocale));
        commandManager.captionRegistry().registerProvider(MinecraftExtrasTranslationBundle.minecraftExtras(SRCommandSender::getLocale));

        MinecraftExceptionHandler
                .create(ComponentHelper::commandSenderToAudience)
                .defaultHandlers()
                .handler(InvalidCommandSenderException.class, (formatter, ctx) -> ComponentHelper.convertJsonToComponent(locale.getMessageRequired(ctx.context().sender(), Message.ONLY_ALLOWED_ON_PLAYER)))
                .handler(SRMessageException.class, (formatter, ctx) -> ComponentHelper.convertJsonToComponent(ctx.exception().getMessageSupplier().apply(locale)))
                .captionFormatter(ComponentCaptionFormatter.miniMessage())
                .registerTo(commandManager);

        commandManager.registerCommandPostProcessor(s -> {
            if (!(s.commandContext().sender() instanceof SRProxyPlayer proxyPlayer)) {
                return;
            }

            if (!settingsManager.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ENABLED)) {
                return;
            }

            Optional<String> currentServer = proxyPlayer.getCurrentServer();
            if (currentServer.isEmpty()) {
                if (!settingsManager.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND)) {
                    proxyPlayer.sendMessage(Message.NOT_CONNECTED_TO_SERVER);
                    ConsumerService.interrupt();
                }

                return;
            }

            boolean inList = settingsManager.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS).contains(currentServer.get());
            boolean shouldBlock = settingsManager.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST) != inList;

            if (shouldBlock) {
                proxyPlayer.sendMessage(Message.COMMAND_SERVER_NOT_ALLOWED_MESSAGE, Placeholder.unparsed("server", currentServer.get()));
                ConsumerService.interrupt();
            }
        });
        commandManager.registerCommandPostProcessor(new CustomCooldownProcessor<>(cooldownManager));

        commandManager.parserRegistry().registerParser(EnumParser.enumParser(SRCommand.PlayerOrSkin.class));
        commandManager.parserRegistry().registerParser(EnumParser.enumParser(SkinVariant.class));
        commandManager.parserRegistry().registerParser(ParserDescriptor.of(new PlayerSelectorArgumentParser(platform), PlayerSelector.class));
        annotationParser.registerBuilderModifier(
                CommandPermission.class,
                (commandPermission, builder) -> builder.permission(PredicatePermission.of(c -> c.hasPermission(commandPermission.value().getPermission())))
        );
        annotationParser.registerBuilderModifier(
                CommandDescription.class,
                (commandDescription, builder) -> builder.commandDescription(Description.of(commandDescription.value().getKey()))
        );
        annotationParser.registerBuilderModifier(
                SRCooldownGroup.class,
                (srCooldownGroup, builder) -> builder.meta(CustomCooldownProcessor.META_COOLDOWN_GROUP, CooldownGroup.named(srCooldownGroup.value()))
        );
        annotationParser.registerBuilderModifier(
                RootDescription.class,
                (rootDescription, builder) -> builder.meta(BUKKIT_DESCRIPTION,
                        ComponentHelper.convertJsonToLegacy(locale.getMessageRequired(locale.getEnglishForeign(), rootDescription.value())))
        );
        commandManager.registerCommandPostProcessor(RequirementPostprocessor.of(
                ConsoleOnlyRequirement.REQUIREMENT_KEY,
                (context, requirement) -> context.sender().sendMessage(Message.ONLY_ALLOWED_ON_CONSOLE)
        ));
        RequirementBindings.create(this.annotationParser, ConsoleOnlyRequirement.REQUIREMENT_KEY).register(
                ConsoleOnly.class,
                annotation -> new ConsoleOnlyRequirement()
        );
    }

    public void registerCommand(Object command) {
        annotationParser.parse(command);
    }

    @SuppressWarnings("DataFlowIssue")
    public void setCooldown(SRPlayer player, CooldownGroup cooldownGroup, Duration duration) {
        final CooldownProfile profile = this.cooldownManager.repository().getProfile(
                player,
                this.cooldownManager.configuration().profileFactory()
        );

        final CooldownInstance instance = CooldownInstance.builder()
                .profile(profile)
                .group(cooldownGroup)
                .duration(duration)
                .creationTime(Instant.now(this.cooldownManager.configuration().clock()))
                .build();
        profile.setCooldown(cooldownGroup, instance);

        this.cooldownManager.configuration().creationListeners().forEach(listener -> listener.cooldownCreated(
                player,
                null,
                instance
        ));
    }

    public void execute(SRCommandSender executor, String input) {
        commandManager.commandExecutor().executeCommand(executor, input);
    }

    private record CooldownMessenger() implements CooldownActiveListener<SRCommandSender> {
        @Override
        public void cooldownActive(@NonNull SRCommandSender sender, @NonNull Command<SRCommandSender> command, @NonNull CooldownInstance cooldown, @NonNull Duration remainingTime) {
            sender.sendMessage(Message.SKIN_COOLDOWN, Placeholder.parsed("time", remainingTime.toString()));
        }
    }
}
