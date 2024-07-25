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

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.commands.library.PlayerSelector;
import net.skinsrestorer.shared.commands.library.SRMessageException;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.utils.UUIDUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class PlayerSelectorArgumentParser implements ArgumentParser<SRCommandSender, PlayerSelector>, BlockingSuggestionProvider<SRCommandSender> {
    private final SRPlatformAdapter platform;

    @Override
    public @NonNull ArgumentParseResult<PlayerSelector> parse(@NonNull CommandContext<@NonNull SRCommandSender> commandContext, @NonNull CommandInput commandInput) {
        final int start = commandInput.cursor();
        final String string = commandInput.readString();

        int current = commandInput.cursor();
        final Collection<SRPlayer> players = platform.getOnlinePlayers(commandContext.sender());
        final List<PlayerSelector.Resolvable> toResolve = new ArrayList<>();

        boolean isFirst = true;
        for (String requestedPlayer : string.split("\\+")) {
            if (isFirst) {
                current += requestedPlayer.length();
            } else {
                current += requestedPlayer.length() + 1;
            }

            isFirst = false;

            if (requestedPlayer.equalsIgnoreCase("-all")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.ALL_PLAYERS));
                continue;
            } else if (requestedPlayer.equalsIgnoreCase("-random")) {
                toResolve.add(new PlayerSelector.Selector(platform, PlayerSelector.SelectorType.RANDOM_PLAYER));
                continue;
            }

            Optional<UUID> parsedUniqueId = UUIDUtils.tryParseUniqueId(requestedPlayer);
            if (parsedUniqueId.isPresent()) {
                toResolve.add(new PlayerSelector.UniqueId(parsedUniqueId.get()));
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

            commandInput.cursor(current - requestedPlayer.length());
            return ArgumentParseResult.failure(new SRMessageException(locale ->
                    locale.getMessageRequired(commandContext.sender(), Message.COMMAND_UNKNOWN_PLAYER, Placeholder.unparsed("name", requestedPlayer))));
        }

        if (!toResolve.isEmpty()) {
            return ArgumentParseResult.success(new PlayerSelector(toResolve));
        }

        commandInput.cursor(start);
        return ArgumentParseResult.failure(new SRMessageException(locale ->
                locale.getMessageRequired(commandContext.sender(), Message.COMMAND_NO_TARGETS_SUPPLIED)));
    }

    @Override
    public @NonNull Iterable<? extends @NonNull Suggestion> suggestions(@NonNull CommandContext<SRCommandSender> context, @NonNull CommandInput input) {
        final Collection<String> usableNames = Stream.concat(
                Stream.of("-all", "-random"),
                platform.getOnlinePlayers(context.sender()).stream()
                        .filter(player -> !(context.sender() instanceof SRPlayer sourcePlayer) || sourcePlayer.canSee(player))
                        .map(SRPlayer::getName)
        ).toList();

        List<Suggestion> suggestions = new ArrayList<>();
        int lastSeparator = input.remainingInput().lastIndexOf('+');
        String lastInput = lastSeparator == -1 ? input.remainingInput() : input.remainingInput().substring(lastSeparator + 1);
        String otherInput = lastSeparator == -1 ? "" : input.remainingInput().substring(0, lastSeparator + 1);
        for (String name : usableNames) {
            if (!name.toLowerCase(Locale.ROOT).startsWith(lastInput.toLowerCase(Locale.ROOT))) {
                continue;
            }

            suggestions.add(Suggestion.suggestion(otherInput + name));
        }

        return suggestions;
    }
}
