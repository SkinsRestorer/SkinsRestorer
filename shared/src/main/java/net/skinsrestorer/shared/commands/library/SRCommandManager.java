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
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinVariant;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.commands.library.types.PlayerSelectorArgumentParser;
import net.skinsrestorer.shared.config.ProxyConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.incendo.cloud.processors.cooldown.profile.CooldownProfile;
import org.incendo.cloud.processors.cooldown.profile.CooldownProfileFactory;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.annotation.RequirementBindings;
import org.incendo.cloud.services.type.ConsumerService;
import org.incendo.cloud.translations.TranslationBundle;
import org.incendo.cloud.translations.minecraft.extras.MinecraftExtrasTranslationBundle;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SRCommandManager {
    public static final CloudKey<String> BUKKIT_DESCRIPTION = CloudKey.of("bukkit_description", String.class);
    @Getter
    private final CommandManager<SRCommandSender> commandManager;
    private final AnnotationParser<SRCommandSender> annotationParser;
    private final CooldownManager<SRCommandSender> cooldownManager;

    @SuppressWarnings({"unchecked", "resource"})
    @Inject
    public SRCommandManager(SRPlatformAdapter platform, SRLogger logger, SkinsRestorerLocale locale, SettingsManager settingsManager, AdapterReference reference) {
        this.commandManager = platform.createCommandManager();
        this.annotationParser = new AnnotationParser<>(commandManager, SRCommandSender.class);
        StorageBackendRepository storageRepository = new StorageBackendRepository(reference);
        CooldownRepository<SRCommandSender> cooldownRepository = CooldownRepository.mapping(
                sender -> {
                    if (sender instanceof SRPlayer player) {
                        return player.getUniqueId();
                    }

                    throw new IllegalArgumentException("Only SRPlayer is supported");
                },
                storageRepository);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SRCooldownCleanupThread");
            t.setDaemon(true);
            return t;
        });
        this.cooldownManager = CooldownManager.cooldownManager(CooldownConfiguration.<SRCommandSender>builder()
                .repository(cooldownRepository)
                .addCreationListener((sender, command, instance) ->
                        service.schedule(new CooldownDeletionTask(instance.group(), instance.profile()), instance.duration().toSeconds(), TimeUnit.SECONDS))
                .addAllActiveCooldownListeners(List.of((sender, command, cooldown, remainingTime) ->
                        sender.sendMessage(Message.SKIN_COOLDOWN, Placeholder.parsed("time", SRHelpers.durationFormat(locale, sender, remainingTime)))))
                .bypassCooldown(context -> !(context.sender() instanceof SRPlayer) || context.sender().hasPermission(PermissionRegistry.BYPASS_COOLDOWN))
                .build());

        try {
            for (UUID cooldownProfile : reference.getOrDefaultThrowing(StorageAdapter::getAllCooldownProfiles, List.<UUID>of())) {
                StorageBackedProfile profile = storageRepository.getProfileIfExists(cooldownProfile);
                for (ImmutableCooldownInstance instance : profile.getAllCooldowns()) {
                    long secondsUntilDeletion = instance.creationTime().plus(instance.duration()).getEpochSecond() - SRHelpers.getEpochSecond();
                    if (secondsUntilDeletion > 0) {
                        service.schedule(new CooldownDeletionTask(instance.group(), profile), secondsUntilDeletion, TimeUnit.SECONDS);
                    } else {
                        profile.deleteCooldown(instance.group());
                    }
                }
            }
        } catch (StorageAdapter.StorageException e) {
            throw new RuntimeException(e);
        }

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
                .handler(InvalidCommandSenderException.class, (formatter, ctx) ->
                        ComponentHelper.convertJsonToComponent(locale.getMessageRequired(ctx.context().sender(), Message.ONLY_ALLOWED_ON_PLAYER)))
                .handler(SRMessageException.class, (formatter, ctx) ->
                        ComponentHelper.convertJsonToComponent(ctx.exception().getMessageSupplier().apply(locale)))
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
                    logger.debug("Player %s is not connected to any server, so the command will be blocked.".formatted(proxyPlayer.getName()));
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

        commandManager.parserRegistry().registerParser(EnumParser.enumParser(SkinVariant.class));
        commandManager.parserRegistry().registerParser(ParserDescriptor.of(new PlayerSelectorArgumentParser(platform), PlayerSelector.class));
        annotationParser.registerBuilderModifier(
                CommandPermission.class,
                (commandPermission, builder) -> builder.permission(PredicatePermission.of(
                        CloudKey.of(commandPermission.value().getPermission().getPermissionString()),
                        c -> c.hasPermission(commandPermission.value())
                ))
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

    @RequiredArgsConstructor
    private static class StorageBackendRepository extends CooldownRepository.AbstractCooldownRepository<UUID> {
        private final AdapterReference reference;

        @Override
        public @NonNull StorageBackedProfile getProfile(@NonNull UUID key, @NonNull CooldownProfileFactory profileFactory) {
            return getProfileIfExists(key);
        }

        @Override
        public @NotNull StorageBackedProfile getProfileIfExists(@NonNull UUID key) {
            return new StorageBackedProfile(key, reference);
        }

        @Override
        public void deleteProfile(@NonNull UUID key) {
            // NO-OP
        }
    }

    private record StorageBackedProfile(UUID key, AdapterReference reference) implements CooldownProfile {
        private static String getCooldownName(CooldownGroup.NamedCooldownGroup group) {
            try {
                Field field = CooldownGroup.NamedCooldownGroup.class.getDeclaredField("name");
                field.setAccessible(true);

                return (String) field.get(group);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public @Nullable CooldownInstance getCooldown(@NonNull CooldownGroup group) {
            String groupName = getCooldownName((CooldownGroup.NamedCooldownGroup) group);
            return getAllCooldowns().stream()
                    .filter(cooldown -> getCooldownName((CooldownGroup.NamedCooldownGroup) cooldown.group()).equals(groupName))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void setCooldown(@NonNull CooldownGroup group, @NonNull CooldownInstance cooldown) {
            String groupName = getCooldownName((CooldownGroup.NamedCooldownGroup) group);
            reference.get().setCooldown(key, groupName, cooldown.creationTime(), cooldown.duration());
        }

        @Override
        public void deleteCooldown(@NonNull CooldownGroup group) {
            String groupName = getCooldownName((CooldownGroup.NamedCooldownGroup) group);
            reference.get().removeCooldown(key, groupName);
        }

        @Override
        public boolean isEmpty() {
            return getAllCooldowns().isEmpty();
        }

        public List<ImmutableCooldownInstance> getAllCooldowns() {
            try {
                return reference.get().getCooldowns(key).stream()
                        .map(storageCooldown -> CooldownInstance.builder()
                                .profile(this)
                                .group(CooldownGroup.named(storageCooldown.groupName()))
                                .duration(storageCooldown.duration())
                                .creationTime(storageCooldown.creationTime())
                                .build())
                        .toList();
            } catch (StorageAdapter.StorageException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private record CooldownDeletionTask(CooldownGroup group, CooldownProfile profile) implements Runnable {
        @Override
        public void run() {
            profile.deleteCooldown(group);
        }
    }
}
