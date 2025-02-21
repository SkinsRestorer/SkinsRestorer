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
package net.skinsrestorer.shared.commands.library;

import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.SRHelpers;

import java.util.*;

public record PlayerSelector(Collection<Resolvable> toResolve) {
    public static PlayerSelector singleton(SRPlayer player) {
        return new PlayerSelector(List.of(new Player(player)));
    }

    public Collection<UUID> resolve(SRCommandSender commandSender) {
        Set<UUID> resolvedPlayers = new LinkedHashSet<>();
        for (Resolvable resolvable : toResolve) {
            resolvedPlayers.addAll(resolvable.resolve(commandSender));
        }

        return resolvedPlayers;
    }

    public enum SelectorType {
        ALL_PLAYERS,
        RANDOM_PLAYER
    }

    public interface Resolvable {
        Collection<UUID> resolve(SRCommandSender commandSender);
    }

    public record Player(SRPlayer player) implements Resolvable {
        @Override
        public Collection<UUID> resolve(SRCommandSender commandSender) {
            return List.of(player.getUniqueId());
        }
    }

    public record UniqueId(UUID uniqueId) implements Resolvable {
        @Override
        public Collection<UUID> resolve(SRCommandSender commandSender) {
            return List.of(uniqueId);
        }
    }

    public record Selector(SRPlatformAdapter platform, SelectorType type) implements Resolvable {
        @Override
        public Collection<UUID> resolve(SRCommandSender commandSender) {
            return switch (type) {
                case ALL_PLAYERS -> platform.getOnlinePlayers(commandSender).stream().map(SRPlayer::getUniqueId).toList();
                case RANDOM_PLAYER -> List.of(SRHelpers.getRandomEntry(platform.getOnlinePlayers(commandSender)).getUniqueId());
            };
        }
    }
}
