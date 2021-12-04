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
import net.skinsrestorer.bukkit.utils.MappingManager;
import net.skinsrestorer.mappings.shared.IMapping;
import net.skinsrestorer.mappings.shared.ViaPacketData;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class MappingSpigotSkinRefresher implements Consumer<Player> {
    private final SkinsRestorer plugin;
    private final IMapping mapping;
    private boolean useViabackwards = false;

    public MappingSpigotSkinRefresher(SkinsRestorer plugin, SRLogger log) throws InitializeException {
        this.plugin = plugin;
        Optional<IMapping> mapping = MappingManager.getMapping();
        if (!mapping.isPresent()) {
            log.severe("No mapping found for this version!");
            throw new InitializeException("No matching mapping found!");
        } else {
            this.mapping = mapping.get();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Wait to run task in order for ViaVersion to determine server protocol
            if (plugin.getServer().getPluginManager().isPluginEnabled("ViaBackwards")
                    && ViaWorkaround.isProtocolNewer()) {
                useViabackwards = true;
                log.debug("Activating ViaBackwards workaround.");
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

        mapping.accept(player, viaFunction);

        if (player.isOp()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Workaround..
                player.setOp(false);
                player.setOp(true);
            });
        }
    }
}
