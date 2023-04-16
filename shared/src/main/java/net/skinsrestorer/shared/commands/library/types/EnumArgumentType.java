/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EnumArgumentType implements ArgumentType<Enum<?>> {
    private final Class<?> enumType;

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String example : getExamples()) {
            if (example.startsWith(builder.getRemaining().toUpperCase(Locale.ENGLISH))) {
                builder.suggest(example);
            }
        }
        return builder.buildFuture();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(enumType.getEnumConstants())
                .map(o -> (Enum) o)
                .map(Enum::name).collect(Collectors.toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Enum<?> parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String string = reader.readString();
        try {
            return Enum.valueOf((Class<Enum>) enumType, string.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            reader.setCursor(start);
            throw new SimpleCommandExceptionType(new LiteralMessage("Invalid enum value")).createWithContext(reader);
        }
    }
}
