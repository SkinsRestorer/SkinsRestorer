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
package net.skinsrestorer.bukkit.refresher;

import net.skinsrestorer.bukkit.mappings.IMapping;
import net.skinsrestorer.bukkit.utils.MappingManager;
import net.skinsrestorer.shared.log.SRLogger;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Optional;

public class MappingSpigotSkinRefresher implements SkinRefresher {
    private final IMapping mapping;
    private final ViaRefreshProvider viaProvider;

    @Inject
    public MappingSpigotSkinRefresher(Server server, SRLogger logger, ViaRefreshProvider viaProvider) {
        this.viaProvider = viaProvider;

        Optional<IMapping> mapping = MappingManager.getMapping(server);
        if (mapping.isEmpty()) {
            logger.severe(String.format("Your Minecraft version (%s) is not supported by this version of SkinsRestorer! Is there a newer version available? If not, join our discord server!",
                    MappingManager.getVersion(server)));
            throw new IllegalStateException("No mapping found for this server version!");
        }

        this.mapping = mapping.get();
    }

    @Override
    public void refresh(Player player) {
        mapping.accept(player, viaProvider);
    }
}
