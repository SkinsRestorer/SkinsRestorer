/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bukkit.skinfactory;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.storage.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Level;

@RequiredArgsConstructor
public class UniversalSkinFactory implements SkinFactory {
    private final SkinsRestorer plugin;
    private final Consumer<Player> refresh = detectRefresh();
    private boolean checkOptFileChecked = false;
    private boolean disableDismountPlayer;
    private boolean enableDismountEntities;
    private boolean enableRemountPlayer;

    @Override
    public void updateSkin(Player player) {
        if (!player.isOnline())
            return;

        if (checkOptFileChecked)
            this.checkOptFile();

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Entity vehicle = player.getVehicle();

            //dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
            if ((Config.DISMOUNT_PLAYER_ON_UPDATE || !disableDismountPlayer) && vehicle != null) {

                vehicle.removePassenger(player);

                if (Config.REMOUNT_PLAYER_ON_UPDATE || enableRemountPlayer) {

                    //this is delayed to next tick to allow the accepter to propagate if necessary (IE: Paper's health update)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        //this is not really necessary, as addPassenger on vanilla despawned vehicles won't do anything, but better to be safe in case the server has plugins that do strange things
                        if (vehicle.isValid()) {

                            vehicle.addPassenger(player);

                        }

                    }, 1);
                }
            }

            //dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
            if ((Config.DISMOUNT_PASSENGERS_ON_UPDATE || enableDismountEntities) && !player.getPassengers().isEmpty()) {

                for (Entity passenger : player.getPassengers()) {
                    player.removePassenger(passenger);
                }
            }

            for (Player ps : Bukkit.getOnlinePlayers()) {
                // Some older spigot versions only support hidePlayer(player)
                try {
                    ps.hidePlayer(this.plugin, player);
                } catch (Exception ignored) {
                    ps.hidePlayer(player);
                }
                try {
                    ps.showPlayer(this.plugin, player);
                } catch (Exception ignored) {
                    ps.showPlayer(player);
                }
            }

            refresh.accept(player);
        });
    }

    private static Consumer<Player> detectRefresh() {
        // Giving warning when using java 9+ regarding illegal reflection access
        final String version = System.getProperty("java.version");
        if (!version.startsWith("1."))
            System.out.println("[SkinsRestorer] [!] WARNING [!] \n[SkinsRestorer] Below message can be IGNORED, we will fix this in a later release!");



        // force OldSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
        // todo: reuse code
        // No need to check for all three Vias as ViaVersion has to be installed for the other two to work.
        // Ran with getPlugin != null instead of isPluginEnabled as older Spigot builds return false during the login process even if enabled
        boolean viaVersion = SkinsRestorer.getInstance().getServer().getPluginManager().getPlugin("ViaVersion") != null;
        boolean protocolSupportExists = SkinsRestorer.getInstance().getServer().getPluginManager().getPlugin("ProtocolSupport") != null;
        if (viaVersion || protocolSupportExists) {
            SkinsRestorer.getInstance().getLogger().log(Level.INFO, "Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing OldSkinRefresher");
            return new OldSkinRefresher();
        }

        try {
            return new PaperSkinRefresher();
        } catch (ExceptionInInitializerError ignored) {
        }

        return new OldSkinRefresher();
    }

    private void checkOptFile() {
        File fileDisableDismountPlayer = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "disablesdismountplayer");
        File fileEnableDismountEntities = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "enablesdismountentities");
        File fileEnableRemountEntiteis = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "enablesremountentities");

        if (fileDisableDismountPlayer.exists())
            disableDismountPlayer = true;

        if (fileEnableDismountEntities.exists())
            enableDismountEntities = true;

        if (fileEnableRemountEntiteis.exists())
            enableRemountPlayer = true;

        checkOptFileChecked = true;
    }
}
