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
package net.skinsrestorer.shared.commands.library;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.SRCommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommandExecutor<T extends SRCommandSender> {
    private final CommandDispatcher<T> dispatcher;
    private final CommandManager<T> manager;
    private final SRCommandMeta meta;

    public void execute(T executor, String input) {
        manager.executeCommand(executor, input);
    }

    public CompletableFuture<List<String>> tabComplete(T executor, String input) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, executor)).thenApply(suggestions ->
                suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList()));
    }

    public boolean hasPermission(T executor) {
        return executor.hasPermission(meta.getRootPermission());
    }
}
