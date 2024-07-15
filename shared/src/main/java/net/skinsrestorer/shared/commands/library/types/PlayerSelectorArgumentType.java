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
package net.skinsrestorer.shared.commands.library.types;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.library.CommandPlatform;
import net.skinsrestorer.shared.commands.library.PlayerSelector;
import net.skinsrestorer.shared.subjects.SRPlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class PlayerSelectorArgumentType implements ArgumentType<PlayerSelector> {
    private final CommandPlatform<?> platform;

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        final Collection<String> usableNames = Stream.concat(
                Stream.of("@a", "@e", "@r", "@s", "@p"),
                platform.getOnlinePlayers().stream()
                        .filter(player -> !(context.getSource() instanceof SRPlayer sourcePlayer) || sourcePlayer.canSee(player))
                        .map(SRPlayer::getName)
        ).toList();

        int lastComma = builder.getRemaining().lastIndexOf(',');
        String lastInput = lastComma == -1 ? builder.getRemaining() : builder.getRemaining().substring(lastComma + 1);
        String otherInput = lastComma == -1 ? "" : builder.getRemaining().substring(0, lastComma + 1);
        for (String name : usableNames) {
            if (!name.toLowerCase(Locale.ROOT).startsWith(lastInput.toLowerCase(Locale.ROOT))) {
                continue;
            }

            builder.suggest(otherInput + name);
        }

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("Pistonmaster", "Pistonmaster,xknat", "@a", "@e", "@r", "@s", "@p");
    }

    @Override
    public PlayerSelector parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String string = reader.readString();

        int current = reader.getCursor();
        final Collection<SRPlayer> players = platform.getOnlinePlayers();
        final List<PlayerSelector.Resolvable> toResolve = new ArrayList<>();

        boolean isFirst = true;
        for (String requestedPlayer : string.split(",")) {
            if (isFirst) {
                current += requestedPlayer.length();
            } else {
                current += requestedPlayer.length() + 1;
            }

            isFirst = false;

            if (requestedPlayer.equalsIgnoreCase("@a")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.ALL_PLAYERS));
                continue;
            } else if (requestedPlayer.equalsIgnoreCase("@e")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.ALL_ENTITIES));
                continue;
            } else if (requestedPlayer.equalsIgnoreCase("@r")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.RANDOM_PLAYER));
                continue;
            } else if (requestedPlayer.equalsIgnoreCase("@s")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.SELF_PLAYER));
                continue;
            } else if (requestedPlayer.equalsIgnoreCase("@p")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.CLOSEST_PLAYER));
                continue;
            }

            Optional<SRPlayer> exactPlayer = players.stream()
                    .filter(p -> p.getName().equals(requestedPlayer))
                    .findFirst();

            if (exactPlayer.isPresent()) {
                toResolve.add(new PlayerSelector.Player(exactPlayer.get()));
                continue;
            }

            Optional<SRPlayer> player = players.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(requestedPlayer))
                    .findFirst();

            if (player.isPresent()) {
                toResolve.add(new PlayerSelector.Player(player.get()));
                continue;
            }

            reader.setCursor(current - requestedPlayer.length());
            throw new SimpleCommandExceptionType(new LiteralMessage("Unknown target")).createWithContext(reader);
        }

        if (!toResolve.isEmpty()) {
            return new PlayerSelector(toResolve);
        }

        reader.setCursor(start);
        throw new SimpleCommandExceptionType(new LiteralMessage("No targets specified")).createWithContext(reader);
    }
}
