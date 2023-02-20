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
import co.aikar.commands.CommandManager;
import co.aikar.commands.SpongeCommandManager;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.val;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.acf.OnlineSRPlayer;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.interfaces.IOExceptionConsumer;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRServerAdapter;
import net.skinsrestorer.sponge.gui.SkinsGUI;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.bstats.sponge.Metrics;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.plugin.PluginContainer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRSpongeAdapter implements SRServerAdapter {
    private final Injector injector;
    private final Metrics metrics;
    @Getter
    private final PluginContainer pluginContainer;
    private final Game game;

    public SRSpongeAdapter(Injector injector, Metrics metrics, PluginContainer container, Game game) {
        this.injector = injector;
        this.metrics = metrics;
        this.pluginContainer = container;
        this.game = game;

        injector.register(SRSpongeAdapter.class, this);
        injector.register(SRServerAdapter.class, this);
        injector.register(Game.class, game);
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
        return false; // TODO: Implement
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
    public CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        SpongeCommandManager manager = new SpongeCommandManager(pluginContainer);

        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);

        val playerResolver = manager.getCommandContexts().getResolver(ServerPlayer.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            ServerPlayer player = (ServerPlayer) playerResolver.getContext(c);
            if (player == null) {
                return null;
            }
            return wrapper.player(player);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandCause.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            CommandCause commandSender = (CommandCause) commandSenderResolver.getContext(c);
            if (commandSender == null) {
                return null;
            }
            return wrapper.commandSender(commandSender);
        });

        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineSRPlayer.class, c -> {
            OnlinePlayer onlinePlayer = (OnlinePlayer) onlinePlayerResolver.getContext(c);
            if (onlinePlayer == null) {
                return null;
            }
            return new OnlineSRPlayer(wrapper.player(onlinePlayer.getPlayer()));
        });

        return manager;
    }

    @Override
    public SRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperSponge.class).commandSender((CommandCause) sender);
    }

    @Override
    public String getPlatformVersion() {
        return game.platform().minecraftVersion().name();
    }

    @Override
    public List<SkinProperty> getPropertiesOfPlayer(SRPlayer player) {
        return player.getAs(Player.class).profile().properties().stream()
                .filter(property -> property.name().equals(SkinProperty.TEXTURES_NAME))
                .filter(ProfileProperty::hasSignature)
                .map(property -> SkinProperty.of(property.value(), property.signature().orElseThrow(() -> new IllegalStateException("Signature is missing"))))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);
        return game.server().onlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }
}
