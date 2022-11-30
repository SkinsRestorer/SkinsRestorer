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
package net.skinsrestorer.bukkit;

import ch.jalu.configme.SettingsManager;
import io.papermc.lib.PaperLib;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.bukkit.events.SkinApplyBukkitEvent;
import net.skinsrestorer.api.interfaces.ISkinApplier;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.reflection.exception.ReflectionException;
import net.skinsrestorer.api.serverinfo.ServerVersion;
import net.skinsrestorer.bukkit.skinrefresher.MappingSpigotSkinRefresher;
import net.skinsrestorer.bukkit.skinrefresher.PaperSkinRefresher;
import net.skinsrestorer.bukkit.skinrefresher.SpigotSkinRefresher;
import net.skinsrestorer.bukkit.utils.BukkitPropertyApplier;
import net.skinsrestorer.bukkit.utils.NoMappingException;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.spigot.SpigotPassengerUtil;
import net.skinsrestorer.spigot.SpigotUtil;
import net.skinsrestorer.v1_7.BukkitLegacyPropertyApplier;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class SkinApplierBukkit implements ISkinApplier {
    @Inject
    private SkinsRestorerBukkit plugin;
    @Inject
    private SRLogger logger;
    @Inject
    private SettingsManager settings;
    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private Consumer<Player> refresh;
    @Setter
    private boolean optFileChecked;
    private boolean disableDismountPlayer;
    private boolean disableRemountPlayer;
    private boolean enableDismountEntities;

    protected Consumer<Player> detectRefresh(Server server) throws InitializeException {
        if (isPaper()) {
            // force SpigotSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
            // Ran with #getPlugin() != null instead of #isPluginEnabled() as older Spigot builds return false during the login process even if enabled
            boolean viaVersionExists = plugin.isPluginEnabled("ViaVersion");
            boolean protocolSupportExists = plugin.isPluginEnabled("ProtocolSupport");
            if (viaVersionExists || protocolSupportExists) {
                logger.debug(SRLogLevel.WARNING, "Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing SpigotSkinRefresher");
                return selectSpigotRefresher(server);
            }

            // use PaperSkinRefresher if no VersionHack plugin found
            try {
                return new PaperSkinRefresher(logger, server);
            } catch (NoMappingException e) {
                throw e;
            } catch (InitializeException e) {
                logger.severe("PaperSkinRefresher failed! (Are you using hybrid software?) Only limited support can be provided. Falling back to SpigotSkinRefresher.");
            }
        }

        return selectSpigotRefresher(server);
    }

    private Consumer<Player> selectSpigotRefresher(Server server) throws InitializeException {
        if (ReflectionUtil.SERVER_VERSION.isNewer(new ServerVersion(1, 17, 1))) {
            return new MappingSpigotSkinRefresher(plugin, logger, server);
        } else return new SpigotSkinRefresher(plugin, logger);
    }

    @Override
    public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
        applySkin(playerWrapper.get(Player.class), property, plugin.getServer());
    }

    /**
     * Applies the skin In other words, sets the skin data, but no changes will
     * be visible until you reconnect or force update with
     *
     * @param player   Player
     * @param property Property Object
     */
    protected void applySkin(Player player, IProperty property, Server server) {
        if (!player.isOnline())
            return;

        plugin.runAsync(() -> {
            SkinApplyBukkitEvent applyEvent = new SkinApplyBukkitEvent(player, property);

            server.getPluginManager().callEvent(applyEvent);

            if (applyEvent.isCancelled())
                return;

            IProperty eventProperty = applyEvent.getProperty();

            if (eventProperty == null)
                return;

            // delay 1 server tick so we override online-mode
            plugin.runSync(() -> {
                applyProperty(player, eventProperty);

                plugin.runAsync(() -> updateSkin(player));
            });
        });
    }

    public void applyProperty(Player player, IProperty property) {
        if (ReflectionUtil.classExists("com.mojang.authlib.GameProfile")) {
            BukkitPropertyApplier.applyProperty(player, property);
        } else {
            BukkitLegacyPropertyApplier.applyProperty(player, property);
        }
    }

    public Map<String, Collection<IProperty>> getPlayerProperties(Player player) throws ReflectionException {
        if (ReflectionUtil.classExists("com.mojang.authlib.GameProfile")) {
            return BukkitPropertyApplier.getPlayerProperties(player);
        } else {
            return BukkitLegacyPropertyApplier.getPlayerProperties(player);
        }
    }

    /**
     * Instantly updates player's skin
     *
     * @param player - Player
     */
    @SuppressWarnings("deprecation")
    public void updateSkin(Player player) {
        if (!player.isOnline())
            return;

        if (!optFileChecked)
            checkOptFile();

        plugin.runSync(() -> {
            if (PaperLib.isSpigot() && SpigotUtil.hasPassengerMethods()) {
                Entity vehicle = player.getVehicle();

                SpigotPassengerUtil.refreshPassengers(plugin.getPluginInstance(), player, vehicle,
                        disableDismountPlayer, disableRemountPlayer, enableDismountEntities, settings);
            }

            for (Player ps : getOnlinePlayers()) {
                // Some older spigot versions only support hidePlayer(player)
                try {
                    ps.hidePlayer(plugin.getPluginInstance(), player);
                } catch (NoSuchMethodError ignored) {
                    ps.hidePlayer(player);
                }

                try {
                    ps.showPlayer(plugin.getPluginInstance(), player);
                } catch (NoSuchMethodError ignored) {
                    ps.showPlayer(player);
                }
            }

            refresh.accept(player);
        });
    }

    private void checkOptFile() {
        Path fileDisableDismountPlayer = plugin.getDataFolder().resolve("disablesdismountplayer"); // legacy
        Path fileDisableRemountPlayer = plugin.getDataFolder().resolve("disablesremountplayer"); // legacy
        Path fileEnableDismountEntities = plugin.getDataFolder().resolve("enablesdismountentities"); // legacy

        Path fileTxtDisableDismountPlayer = plugin.getDataFolder().resolve("disableDismountPlayer.txt");
        Path fileTxtDisableRemountPlayer = plugin.getDataFolder().resolve("disableRemountPlayer.txt");
        Path fileTxtEnableDismountEntities = plugin.getDataFolder().resolve("enableDismountEntities.txt");

        if (Files.exists(fileDisableDismountPlayer)) {
            try {
                Files.move(fileDisableDismountPlayer, fileTxtDisableDismountPlayer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Files.exists(fileDisableRemountPlayer)) {
            try {
                Files.move(fileDisableRemountPlayer, fileTxtDisableRemountPlayer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Files.exists(fileEnableDismountEntities)) {
            try {
                Files.move(fileEnableDismountEntities, fileTxtEnableDismountEntities);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        disableDismountPlayer = Files.exists(fileTxtDisableDismountPlayer);
        disableRemountPlayer = Files.exists(fileTxtDisableRemountPlayer);
        enableDismountEntities = Files.exists(fileTxtEnableDismountEntities);

        logger.debug("[Debug] Opt Files: { disableDismountPlayer: " + disableDismountPlayer + ", disableRemountPlayer: " + disableRemountPlayer + ", enableDismountEntities: " + enableDismountEntities + " }");
        optFileChecked = true;
    }

    private boolean isPaper() {
        if (PaperLib.isPaper() && ReflectionUtil.SERVER_VERSION.isNewer(new ServerVersion(1, 11, 2))) {
            if (hasPaperMethods()) {
                return true;
            } else {
                logger.debug(SRLogLevel.WARNING, "Paper detected, but the methods are missing. Disabling Paper Refresher.");
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean hasPaperMethods() {
        try {
            ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }

    private Collection<? extends Player> getOnlinePlayers() {
        try {
            return com.github.puregero.multilib.MultiLib.getAllOnlinePlayers();
        } catch (UnsupportedClassVersionError e) {
            return plugin.getServer().getOnlinePlayers();
        }
    }
}
