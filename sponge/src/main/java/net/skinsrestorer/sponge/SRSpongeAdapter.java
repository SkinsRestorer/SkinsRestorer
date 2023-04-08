/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.sponge;

import ch.jalu.injector.Injector;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.commands.library.SRRegisterPayload;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.IOExceptionConsumer;
import net.skinsrestorer.sponge.gui.SkinsGUI;
import net.skinsrestorer.sponge.listeners.ForceAliveListener;
import net.skinsrestorer.sponge.wrapper.WrapperSponge;
import org.bstats.sponge.Metrics;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.plugin.PluginContainer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRSpongeAdapter implements SRServerAdapter<PluginContainer> {
    private final Injector injector;
    private final Metrics metrics;
    @Getter
    private final PluginContainer pluginContainer;
    private final Game game;
    private final Set<SRRegisterPayload<SRCommandSender>> commands = new HashSet<>();

    public SRSpongeAdapter(Injector injector, Metrics metrics, PluginContainer container) {
        this.injector = injector;
        this.metrics = metrics;
        this.pluginContainer = container;
        this.game = injector.getSingleton(Game.class);

        injector.register(SRSpongeAdapter.class, this);
        injector.register(SRServerAdapter.class, this);

        game.eventManager().registerListeners(pluginContainer, this);
    }

    @Override
    public Object createMetricsInstance() {
        return metrics;
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return game.pluginManager().plugin(pluginName).isPresent();
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        game.asyncScheduler().executor(pluginContainer).execute(runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        game.server().scheduler().executor(pluginContainer).execute(runnable);
    }

    @Override
    public boolean determineProxy() {
        PluginContainer sponge = Sponge.pluginManager().plugin("sponge").orElseThrow(IllegalStateException::new);

        try {
            String mode = Sponge.configManager().pluginConfig(sponge)
                    .config().load().node("ip-forwarding", "mode")
                    .getString();

            if (mode == null) {
                throw new IllegalStateException("Invalid config");
            }

            return !mode.equals("NONE");
        } catch (ConfigurateException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void openServerGUI(SRPlayer player, int page) {
        InventoryMenu inventory = injector.getSingleton(SharedGUI.class)
                .createGUI(injector.getSingleton(SkinsGUI.class), injector.getSingleton(SharedGUI.ServerGUIActions.class), player, page);

        runSync(() -> inventory.open(player.getAs(ServerPlayer.class)));
    }

    @Override
    public void openProxyGUI(SRPlayer player, int page, Map<String, String> skinList) {
        InventoryMenu inventory = injector.getSingleton(SkinsGUI.class)
                .createGUI(injector.getSingleton(SharedGUI.ProxyGUIActions.class), player, page, skinList);

        runSync(() -> inventory.open(player.getAs(ServerPlayer.class)));
    }

    @Override
    public Optional<SRPlayer> getPlayer(String name) {
        return game.server().player(name).map(p -> injector.getSingleton(WrapperSponge.class).player(p));
    }

    @Override
    public void sendToMessageChannel(SRPlayer player, IOExceptionConsumer<DataOutputStream> consumer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            consumer.accept(out);

            game.channelManager().ofType(ResourceKey.of("sr", "messagechannel"), RawDataChannel.class)
                    .play().sendTo(player.getAs(ServerPlayer.class), buf -> buf.writeBytes(bytes.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        game.asyncScheduler().executor(pluginContainer).scheduleAtFixedRate(runnable, delay, interval, timeUnit);
    }

    @Override
    public void extendLifeTime(PluginContainer plugin, Object object) {
        game.eventManager().registerListener(EventListenerRegistration
                .builder(ConstructPluginEvent.class)
                .order(Order.POST)
                .plugin(plugin)
                .listener(new ForceAliveListener(object)).build());
    }

    @Override
    public String getPlatformVersion() {
        return game.platform().minecraftVersion().name();
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return player.getAs(Player.class).profile().properties().stream()
                .filter(property -> property.name().equals(SkinProperty.TEXTURES_NAME))
                .filter(ProfileProperty::hasSignature)
                .map(property -> SkinProperty.of(property.value(), property.signature().orElseThrow(() -> new IllegalStateException("Signature is missing"))))
                .findFirst();
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);
        return game.server().onlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public void registerCommand(SRRegisterPayload<SRCommandSender> payload) {
        commands.add(payload);
    }

    @Listener
    public void onCommandRegister(RegisterCommandEvent<Command.Raw> event) {
        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);
        for (SRRegisterPayload<SRCommandSender> payload : commands) {
            event.register(pluginContainer, new Command.Raw() {
                @Override
                public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
                    String argumentsString = arguments.remaining();
                    String command = payload.getMeta().getRootName() + (argumentsString.isEmpty() ? "" : " " + argumentsString);
                    payload.getExecutor().execute(wrapper.commandSender(cause), command);
                    return CommandResult.builder().build();
                }

                @Override
                public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
                    return payload.getExecutor().tabComplete(wrapper.commandSender(cause), arguments.remaining()).thenApply(list -> list.stream()
                            .map(CommandCompletion::of)
                            .collect(Collectors.toList())).join();
                }

                @Override
                public boolean canExecute(CommandCause cause) {
                    return payload.getExecutor().hasPermission(wrapper.commandSender(cause));
                }

                @Override
                public Optional<Component> shortDescription(CommandCause cause) {
                    return Optional.of(Component.text(payload.getMeta().getDescription()));
                }

                @Override
                public Optional<Component> extendedDescription(CommandCause cause) {
                    return Optional.empty();
                }

                @Override
                public Component usage(CommandCause cause) {
                    return Component.empty(); // TODO
                }
            }, payload.getMeta().getRootName(), payload.getMeta().getAliases());
        }
    }
}
