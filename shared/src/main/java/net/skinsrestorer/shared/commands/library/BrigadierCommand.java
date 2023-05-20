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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRCommandSender;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@RequiredArgsConstructor
public class BrigadierCommand<T extends SRCommandSender> implements Command<T> {
    private final Method method;
    private final SRLogger logger;
    private final Object command;
    private final CommandPlatform<T> platform;

    @Override
    public int run(CommandContext<T> context) throws CommandSyntaxException {
        try {
            int i1 = 0;
            Object[] parameters = new Object[method.getParameterCount()];
            for (Parameter parameter : method.getParameters()) {
                if (i1 == 0) {
                    parameters[i1] = context.getSource();
                    i1++;
                    continue;
                }
                parameters[i1] = context.getArgument(parameter.getName(), parameter.getType());
                i1++;
            }
            logger.debug(String.format("Executing command %s with method parameters %s", method.getName(), Arrays.toString(parameters)));
            platform.runAsync(() -> {
                try {
                    method.invoke(command, parameters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
