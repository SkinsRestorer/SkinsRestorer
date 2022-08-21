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
package net.skinsrestorer.shared.commands;

import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;

import java.util.Optional;

public class CommandUtil {
    public static boolean isAllowedToExecute(ISRCommandSender sender) {
        if (Config.NOT_ALLOWED_COMMAND_SERVERS_ENABLED && sender instanceof ISRProxyPlayer) {
            Optional<String> optional = ((ISRProxyPlayer) sender).getCurrentServer();
            if (optional.isPresent()) {
                String server = optional.get();

                if (Config.NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST) {
                    if (Config.NOT_ALLOWED_COMMAND_SERVERS.contains(server)) {
                        return true;
                    } else {
                        sender.sendMessage(Locale.COMMAND_SERVER_NOT_ALLOWED_MESSAGE, server);
                        return false;
                    }
                } else {
                    if (Config.NOT_ALLOWED_COMMAND_SERVERS.contains(server)) {
                        sender.sendMessage(Locale.COMMAND_SERVER_NOT_ALLOWED_MESSAGE, server);
                        return false;
                    }
                }
            } else return !Config.NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND;
        }

        return true;
    }
}
