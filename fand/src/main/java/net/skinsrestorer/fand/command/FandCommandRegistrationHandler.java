/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.fand.command;

import io.fand.api.command.Arguments;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandRegistry;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.util.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class FandCommandRegistrationHandler<C> implements CommandRegistrationHandler<C> {
    private final CommandRegistry commandRegistry;
    private final Set<String> registeredRoots = new HashSet<>();
    private FandCommandManager<C> manager;

    FandCommandRegistrationHandler(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
    }

    void initialize(FandCommandManager<C> manager) {
        this.manager = manager;
    }

    @Override
    public boolean registerCommand(Command<C> command) {
        CommandComponent<C> root = command.rootComponent();
        if (!registeredRoots.add(root.name())) {
            return true;
        }
        commandRegistry.register(root.name(), builder -> {
            builder.namespace("skinsrestorer").aliases(root.alternativeAliases());
            builder.executes(context -> execute(root.name(), context));
            builder.argument("arguments", Arguments.greedyString().asOptional(), branch -> branch
                    .executes(context -> execute(root.name(), context))
                    .suggests(context -> suggest(root.name(), context)));
        });
        return true;
    }

    private void execute(String root, CommandContext context) {
        manager.commandExecutor().executeCommand(
                manager.senderMapper().map(context.sender()),
                commandInput(root, context));
    }

    private java.util.List<String> suggest(String root, CommandContext context) {
        String input = commandInput(root, context);
        var suggestions = manager.suggestionFactory().suggestImmediately(
                manager.senderMapper().map(context.sender()),
                input);
        return suggestions.list().stream()
                .map(Suggestion::suggestion)
                .map(suggestion -> StringUtils.trimBeforeLastSpace(suggestion, suggestions.commandInput()))
                .filter(Objects::nonNull)
                .toList();
    }

    private static String commandInput(String root, CommandContext context) {
        return context.args().isEmpty() ? root : root + " " + String.join(" ", context.args());
    }
}
