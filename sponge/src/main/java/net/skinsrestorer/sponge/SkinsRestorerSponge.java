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

import co.aikar.commands.CommandManager;
import co.aikar.commands.SpongeCommandManager;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.val;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.commands.OnlineISRPlayer;
import net.skinsrestorer.shared.commands.SRCommand;
import net.skinsrestorer.shared.commands.SkinCommand;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerServerShared;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.Slf4jLoggerImpl;
import net.skinsrestorer.sponge.listeners.LoginListener;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.bstats.sponge.Metrics;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.property.ProfileProperty;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerSponge extends SkinsRestorerServerShared {
    private final Object pluginInstance; // Only for platform API use
    private final Metrics.Factory metricsFactory;
    private final PluginContainer pluginContainer;
    protected Game game;

    public SkinsRestorerSponge(Object pluginInstance, Metrics.Factory metricsFactory, Path dataFolder, Logger log, PluginContainer container) {
        super(
                new Slf4jLoggerImpl(log),
                false,
                container.getVersion().orElse("Unknown"),
                "SkinsRestorerUpdater/Sponge",
                dataFolder,
                new WrapperFactorySponge(),
                GenericProperty::new
        );
        injector.register(SkinsRestorerSponge.class, this);
        this.pluginInstance = pluginInstance;
        this.metricsFactory = metricsFactory;
        this.pluginContainer = container;
    }

    @Override
    protected void pluginStartup() {
        startupStart();

        updateCheck();

        // Init config files
        loadConfig();
        loadLocales();

        initMineSkinAPI();

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        SkinApplierSponge skinApplierSponge = injector.getSingleton(SkinApplierSponge.class);

        // Init API
        registerAPI(skinApplierSponge);

        Sponge.getEventManager().registerListener(pluginInstance, ClientConnectionEvent.Auth.class, injector.newInstance(LoginListener.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands();

        manager.registerCommand(injector.getSingleton(SkinCommand.class));
        manager.registerCommand(injector.newInstance(SRCommand.class));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPI.class), logger));
    }

    @Override
    protected Object createMetricsInstance() {
        return metricsFactory.make(2337);
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return Sponge.getPluginManager().isLoaded(pluginName);
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
        Sponge.getScheduler().createSyncExecutor(pluginInstance).execute(runnable);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        game.getScheduler().createTaskBuilder().execute(runnable).interval(interval, timeUnit).delay(delay, timeUnit).submit(pluginInstance);
    }

    @Override
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        SpongeCommandManager manager = new SpongeCommandManager(pluginContainer);

        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);

        val playerResolver = manager.getCommandContexts().getResolver(Player.class);
        manager.getCommandContexts().registerIssuerAwareContext(ISRPlayer.class, c -> {
            Object playerObject = playerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return wrapper.player((Player) playerObject);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSource.class);
        manager.getCommandContexts().registerIssuerAwareContext(ISRCommandSender.class, c -> {
            Object commandSenderObject = commandSenderResolver.getContext(c);
            if (commandSenderObject == null) {
                return null;
            }
            return wrapper.commandSender((CommandSource) commandSenderObject);
        });

        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineISRPlayer.class, c -> {
            Object playerObject = onlinePlayerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return new OnlineISRPlayer(wrapper.player(((OnlinePlayer) playerObject).getPlayer()));
        });

        return manager;
    }

    @Override
    public String getPlatformVersion() {
        return game.getPlatform().getMinecraftVersion().getName();
    }

    @Override
    public String getProxyMode() {
        return "Sponge-Plugin";
    }

    @Override
    public List<IProperty> getPropertiesOfPlayer(ISRPlayer player) {
        Collection<ProfileProperty> properties = player.getWrapper().get(Player.class).getProfile().getPropertyMap().get(IProperty.TEXTURES_NAME);
        return properties.stream()
                .map(property -> new GenericProperty(property.getName(), property.getValue(), property.getSignature().orElse("")))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);
        return game.getServer().getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    private static class WrapperFactorySponge implements IWrapperFactory {
        @Override
        public String getPlayerName(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

                return player.getName();
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }
}
