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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRCommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommandExecutor<T extends SRCommandSender> {
    private final CommandDispatcher<T> dispatcher;
    @Getter
    private final CommandManager<T> manager;
    private final SRCommandMeta meta;
    private final SRLogger logger;

    public void execute(T executor, String input) {
        manager.executeCommand(executor, input);
    }

    public CompletableFuture<List<String>> tabComplete(T executor, String input) {
        logger.debug(String.format("Tab completing: '%s' for '%s'", input, executor.getName()));
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, executor)).thenApply(suggestions ->
                suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList()));
    }

    public boolean hasPermission(T executor) {
        logger.debug(String.format("Checking permission: '%s' for '%s'", meta.getRootPermission(), executor.getName()));
        return executor.hasPermission(meta.getRootPermission());
    }

    public String getHelpUsage(T executor) {
        return Arrays.stream(dispatcher.getAllUsage(dispatcher.getRoot().getChild(meta.getRootName()), executor, true))
                .filter(s -> !s.isEmpty())
                .map(s -> "/" + meta.getRootName() + " " + s)
                .collect(Collectors.joining("\n"));
    }
}
