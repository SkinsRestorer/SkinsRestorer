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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record PlayerSelector(Collection<Resolvable> toResolve) {
    public static PlayerSelector singleton(SRPlayer player) {
        return new PlayerSelector(List.of(new Player(player)));
    }

    public Collection<SRPlayer> resolve(SRCommandSender commandSender) {
        Set<SRPlayer> resolvedPlayers = new LinkedHashSet<>();
        for (Resolvable resolvable : toResolve) {
            resolvedPlayers.addAll(resolvable.resolve(commandSender));
        }

        return resolvedPlayers;
    }

    public enum SelectorType {
        ALL_PLAYERS,
        ALL_ENTITIES,
        RANDOM_PLAYER,
        SELF_PLAYER,
        CLOSEST_PLAYER
    }

    public interface Resolvable {
        Collection<SRPlayer> resolve(SRCommandSender commandSender);
    }

    public record Player(SRPlayer player) implements Resolvable {
        @Override
        public Collection<SRPlayer> resolve(SRCommandSender commandSender) {
            return List.of(player);
        }
    }

    public record Selector(SRPlatformAdapter platform, SelectorType type) implements Resolvable {
        @Override
        public Collection<SRPlayer> resolve(SRCommandSender commandSender) {
            return switch (type) {
                case ALL_PLAYERS, ALL_ENTITIES -> platform.getOnlinePlayers();
                case RANDOM_PLAYER -> List.of(SRHelpers.getRandomEntry(platform.getOnlinePlayers()));
                case SELF_PLAYER, CLOSEST_PLAYER ->
                        commandSender instanceof SRPlayer player ? List.of(player) : List.of();
            };
        }
    }
}
