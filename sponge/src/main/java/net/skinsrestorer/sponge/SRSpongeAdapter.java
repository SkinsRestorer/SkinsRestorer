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
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRServerAdapter;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.bstats.sponge.Metrics;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.property.ProfileProperty;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRSpongeAdapter implements SRServerAdapter {
    private final Injector injector;
    @Getter
    private final Object pluginInstance; // Only for platform API use
    private final Metrics.Factory metricsFactory;
    private final PluginContainer pluginContainer;
    private final Game game;

    public SRSpongeAdapter(Injector injector, Object pluginInstance, Metrics.Factory metricsFactory, PluginContainer container, Game game) {
        this.injector = injector;
        this.pluginInstance = pluginInstance;
        this.metricsFactory = metricsFactory;
        this.pluginContainer = container;
        this.game = game;

        injector.register(SRSpongeAdapter.class, this);
        injector.register(SRServerAdapter.class, this);
        injector.register(Game.class, game);
    }

    @Override
    public Object createMetricsInstance() {
        return metricsFactory.make(2337);
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return game.getPluginManager().isLoaded(pluginName);
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        game.getScheduler().createAsyncExecutor(pluginInstance).execute(runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        game.getScheduler().createSyncExecutor(pluginInstance).execute(runnable);
    }

    @Override
    public boolean determineProxy() {
        return false; // TODO: Implement
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        game.getScheduler().createTaskBuilder().execute(runnable).interval(interval, timeUnit).delay(delay, timeUnit).submit(pluginInstance);
    }

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        SpongeCommandManager manager = new SpongeCommandManager(pluginContainer);

        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);

        val playerResolver = manager.getCommandContexts().getResolver(Player.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            Player player = (Player) playerResolver.getContext(c);
            if (player == null) {
                return null;
            }
            return wrapper.player(player);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSource.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            CommandSource commandSender = (CommandSource) commandSenderResolver.getContext(c);
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
        return injector.getSingleton(WrapperSponge.class).commandSender((CommandSource) sender);
    }

    @Override
    public String getPlatformVersion() {
        return game.getPlatform().getMinecraftVersion().getName();
    }

    @Override
    public List<SkinProperty> getPropertiesOfPlayer(SRPlayer player) {
        Collection<ProfileProperty> properties = player.getAs(Player.class).getProfile().getPropertyMap().get(SkinProperty.TEXTURES_NAME);
        return properties.stream()
                .map(property -> SkinProperty.of(property.getValue(), property.getSignature().orElseThrow(() -> new IllegalStateException("Signature is missing"))))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);
        return game.getServer().getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }
}
