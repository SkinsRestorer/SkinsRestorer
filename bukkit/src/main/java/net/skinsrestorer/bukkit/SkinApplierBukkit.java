/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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

import com.mojang.authlib.GameProfile;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.bukkit.events.SkinApplyBukkitEvent;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.reflection.exception.ReflectionException;
import net.skinsrestorer.api.serverinfo.ServerVersion;
import net.skinsrestorer.bukkit.skinrefresher.MappingSpigotSkinRefresher;
import net.skinsrestorer.bukkit.skinrefresher.PaperSkinRefresher;
import net.skinsrestorer.bukkit.skinrefresher.SpigotSkinRefresher;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SkinApplierBukkit {
    @Setter
    private static boolean optFileChecked;
    private static boolean disableDismountPlayer;
    private static boolean disableRemountPlayer;
    private static boolean enableDismountEntities;
    private final SkinsRestorer plugin;
    private final SRLogger log;
    @Getter
    private final Consumer<Player> refresh;

    public SkinApplierBukkit(SkinsRestorer plugin, SRLogger log) throws InitializeException {
        this.plugin = plugin;
        this.log = log;
        refresh = detectRefresh();
    }

    private Consumer<Player> detectRefresh() throws InitializeException {
        if (isPaper()) {
            // force SpigotSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
            // Ran with #getPlugin() != null instead of #isPluginEnabled() as older Spigot builds return false during the login process even if enabled
            boolean viaVersionExists = plugin.getServer().getPluginManager().getPlugin("ViaVersion") != null;
            boolean protocolSupportExists = plugin.getServer().getPluginManager().getPlugin("ProtocolSupport") != null;
            if (viaVersionExists || protocolSupportExists) {
                log.debug(SRLogLevel.WARNING, "Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing SpigotSkinRefresher");
                return selectSpigotRefresher();
            }

            // use PaperSkinRefresher if no VersionHack plugin found
            try {
                return new PaperSkinRefresher(log);
            } catch (InitializeException e) {
                e.printStackTrace();
                log.severe("PaperSkinRefresher failed! (Are you using hybrid software?) Only limited support can be provided. Falling back to SpigotSkinRefresher.");
            }
        }

        return selectSpigotRefresher();
    }

    private Consumer<Player> selectSpigotRefresher() throws InitializeException {
        if (ReflectionUtil.SERVER_VERSION.isNewer(new ServerVersion(1, 17, 1))) {
            return new MappingSpigotSkinRefresher(plugin, log);
        } else return new SpigotSkinRefresher(plugin, log);
    }

    /**
     * Applies the skin In other words, sets the skin data, but no changes will
     * be visible until you reconnect or force update with
     *
     * @param player   Player
     * @param property Property Object
     */
    protected void applySkin(Player player, IProperty property) {
        if (!player.isOnline())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinApplyBukkitEvent applyEvent = new SkinApplyBukkitEvent(player, property);

            Bukkit.getPluginManager().callEvent(applyEvent);

            if (applyEvent.isCancelled())
                return;

            if (property == null)
                return;

            // delay 1 server tick so we override online-mode
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                applyProperty(player, property);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> updateSkin(player));
            });
        });
    }

    @SuppressWarnings("unchecked")
    public void applyProperty(Player player, IProperty property) {
        try {
            GameProfile profile = getGameProfile(player);
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", property);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    public GameProfile getGameProfile(Player player) throws ReflectionException {
        Object ep = ReflectionUtil.invokeMethod(player.getClass(), player, "getHandle");
        GameProfile profile;
        try {
            profile = (GameProfile) ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
        } catch (Exception e) {
            profile = (GameProfile) ReflectionUtil.getFieldByType(ep, "GameProfile");
        }
      
        return profile;
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

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Entity vehicle = player.getVehicle();

            // Dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
            if ((Config.DISMOUNT_PLAYER_ON_UPDATE && !disableDismountPlayer) && vehicle != null) {
                vehicle.removePassenger(player);

                if (Config.REMOUNT_PLAYER_ON_UPDATE && !disableRemountPlayer) {
                    // This is delayed to next tick to allow the accepter to propagate if necessary (IE: Paper's health update)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        // This is not really necessary, as addPassenger on vanilla despawned vehicles won't do anything, but better to be safe in case the server has plugins that do strange things
                        if (vehicle.isValid()) {
                            vehicle.addPassenger(player);
                        }
                    }, 1);
                }
            }

            // Dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
            if ((Config.DISMOUNT_PASSENGERS_ON_UPDATE || enableDismountEntities) && !player.isEmpty()) {
                for (Entity passenger : player.getPassengers()) {
                    player.removePassenger(passenger);
                }
            }

            for (Player ps : Bukkit.getOnlinePlayers()) {
                // Some older spigot versions only support hidePlayer(player)
                try {
                    ps.hidePlayer(plugin, player);
                } catch (NoSuchMethodError ignored) {
                    ps.hidePlayer(player);
                }

                try {
                    ps.showPlayer(plugin, player);
                } catch (NoSuchMethodError ignored) {
                    ps.showPlayer(player);
                }
            }

            refresh.accept(player);
        });
    }

    private void checkOptFile() {
        File fileDisableDismountPlayer = new File(plugin.getDataFolder(), "disablesdismountplayer");
        File fileDisableRemountPlayer = new File(plugin.getDataFolder(), "disablesremountplayer");
        File fileEnableDismountEntities = new File(plugin.getDataFolder(), "enablesdismountentities");
        File fileTxtDisableDismountPlayer = new File(plugin.getDataFolder(), "disableDismountPlayer.txt");
        File fileTxtDisableRemountPlayer = new File(plugin.getDataFolder(), "disableRemountPlayer.txt");
        File fileTxtEnableDismountEntities = new File(plugin.getDataFolder(), "enableDismountEntities.txt");

        disableDismountPlayer = fileDisableDismountPlayer.exists() || fileTxtDisableDismountPlayer.exists();
        disableRemountPlayer = fileDisableRemountPlayer.exists() || fileTxtDisableRemountPlayer.exists();
        enableDismountEntities = fileEnableDismountEntities.exists() || fileTxtEnableDismountEntities.exists();

        log.debug("[Debug] Opt Files: { disableDismountPlayer: " + disableDismountPlayer + ", disableRemountPlayer: " + disableRemountPlayer + ", enableDismountEntities: " + enableDismountEntities + " }");
        optFileChecked = true;
    }

    private boolean isPaper() {
        if (PaperLib.isPaper() && ReflectionUtil.SERVER_VERSION.isNewer(new ServerVersion(1, 11, 2))) {
            if (hasPaperMethods()) {
                return true;
            } else {
                log.debug(SRLogLevel.WARNING, "Paper detected, but the methods are missing. Disabling Paper Refresher.");
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
}
