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

import ch.jalu.injector.Injector;
import net.skinsrestorer.bukkit.mappings.IMapping;
import net.skinsrestorer.bukkit.mappings.ViaPacketData;
import net.skinsrestorer.bukkit.utils.ExceptionSupplier;
import net.skinsrestorer.bukkit.utils.MappingManager;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.messages.Message;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class MappingSpigotSkinRefresher implements SkinRefresher {
    private final IMapping mapping;
    private final ViaRefreshProvider viaProvider;

    @Inject
    public MappingSpigotSkinRefresher(Injector injector, Server server, SRLogger logger, ViaRefreshProvider viaProvider) {
        this.viaProvider = viaProvider;

        Optional<IMapping> mapping = MappingManager.getMapping(server);
        if (mapping.isEmpty()) {
            logger.severe(("Your Minecraft version (%s) is not supported by this version of SkinsRestorer! " +
                    "Is there a newer version available? " +
                    "If not, join our discord server!").formatted(MappingManager.getVersion(server)));

            if (Boolean.getBoolean("sr.throw-if-mapping-unsupported")) {
                throw new IllegalStateException("Unsupported Minecraft version");
            } else {
                this.mapping = injector.getSingleton(UnsupportedMapping.class);
            }
        } else {
            this.mapping = mapping.get();
        }
    }

    @Override
    public void refresh(Player player) {
        mapping.accept(player, viaProvider);
    }

    private record UnsupportedMapping(WrapperBukkit wrapper) implements IMapping {
        @Inject
        private UnsupportedMapping {
        }

        @Override
        public void accept(Player player, Predicate<ExceptionSupplier<ViaPacketData>> viaFunction) {
            wrapper.player(player).sendMessage(Message.ERROR_PLAYER_REFRESH_NO_MAPPING);
        }

        @Override
        public Set<String> getSupportedVersions() {
            return Set.of(); // This is fine, it's not used
        }
    }
}
