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
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.FluentList;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class SRPlayerArgumentType implements ArgumentType<SRPlayer> {
    private final CommandPlatform<?> platform;

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (SRPlayer player : platform.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(builder.getRemaining().toLowerCase(Locale.ROOT))) {
                if (context.getSource() instanceof SRPlayer && !((SRPlayer) context.getSource()).canSee(player)) {
                    continue;
                }

                builder.suggest(player.getName());
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return FluentList.of("Pistonmaster", "xknat");
    }

    @Override
    public SRPlayer parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String string = reader.readString();

        Optional<SRPlayer> exactPlayer = platform.getOnlinePlayers().stream()
                .filter(p -> p.getName().equals(string))
                .findFirst();

        if (exactPlayer.isPresent()) {
            return exactPlayer.get();
        }

        Optional<SRPlayer> player = platform.getOnlinePlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(string))
                .findFirst();

        if (player.isPresent()) {
            return player.get();
        }

        reader.setCursor(start);
        throw new SimpleCommandExceptionType(new LiteralMessage("Unknown player")).createWithContext(reader);
    }
}
