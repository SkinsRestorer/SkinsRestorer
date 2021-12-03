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
package net.skinsrestorer.bukkit.skinrefresher;

import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.mappings.mapping1_18.Mapping1_18;
import net.skinsrestorer.mappings.mapping1_18.ViaPacketData;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Function;

public class MappingSpigotSkinRefresher implements Consumer<Player> {
    private final SkinsRestorer plugin;
    private boolean useViabackwards = false;

    public MappingSpigotSkinRefresher(SkinsRestorer plugin, SRLogger log) throws InitializeException {
        this.plugin = plugin;

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Wait to run task in order for ViaVersion to determine server protocol
            if (plugin.getServer().getPluginManager().isPluginEnabled("ViaBackwards")
                    && ViaWorkaround.isProtocolNewer()) {
                useViabackwards = true;
                log.info("Activating ViaBackwards workaround.");
            }
        });

        log.debug("Using MappingSpigotSkinRefresher");
    }


    @Override
    public void accept(Player player) {
        Function<ViaPacketData, Boolean> viaFunction;

        if (useViabackwards) {
            viaFunction = ViaWorkaround::sendCustomPacketVia;
        } else {
            viaFunction = data -> true;
        }

        Mapping1_18.accept(player, viaFunction);

        if (player.isOp()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Workaround..
                player.setOp(false);
                player.setOp(true);
            });
        }
    }
}
