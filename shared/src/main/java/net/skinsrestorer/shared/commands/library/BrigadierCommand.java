/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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

import ch.jalu.configme.SettingsManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.Message;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@RequiredArgsConstructor
public class BrigadierCommand<T extends SRCommandSender> implements Command<T> {
    private final Method method;
    private final SRLogger logger;
    private final Object command;
    private final CommandPlatform<T> platform;
    private final SettingsManager settingsManager;

    @Override
    public int run(CommandContext<T> context) throws CommandSyntaxException {
        try {
            Object[] parameters = new Object[method.getParameterCount()];

            int i = 0;
            for (Parameter parameter : method.getParameters()) {
                // Add the command source as the first parameter
                if (i == 0) {
                    parameters[i] = context.getSource();
                    i++;
                    continue;
                }

                Object value = context.getArgument(parameter.getName(), parameter.getType());

                if (value instanceof String) {
                    value = handleStringArgument(context.getSource(), (String) value);
                }

                parameters[i] = value;
                i++;
            }
            logger.debug(String.format("Executing command %s with method parameters %s", method.getName(), Arrays.toString(parameters)));
            platform.runAsync(() -> {
                try {
                    method.invoke(command, parameters);
                } catch (Exception e) {
                    logger.severe("Error while executing command " + method.getName(), e);
                }
            });

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            logger.severe("Error while executing command " + method.getName(), e);
            return 0;
        }
    }

    private String handleStringArgument(T source, String argument) {
        if (settingsManager.getProperty(CommandConfig.REMOVE_BRACKETS) &&
                ((argument.startsWith("<") && argument.endsWith(">")) || (argument.startsWith("[") && argument.endsWith("]")))) {
            source.sendMessage(Message.HELP_NO_BRACKETS);
            return argument.substring(1, argument.length() - 1);
        }

        return argument;
    }
}
