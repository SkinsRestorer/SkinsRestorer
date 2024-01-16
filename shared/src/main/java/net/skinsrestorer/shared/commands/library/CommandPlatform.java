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
package net.skinsrestorer.shared.commands.library;

import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;

import java.util.Collection;

public interface CommandPlatform<T> {
    void registerCommand(SRRegisterPayload<T> payload);

    void runAsync(Runnable runnable);

    Collection<SRPlayer> getOnlinePlayers();

    SRCommandSender convertPlatformSender(T sender);

    Class<T> getPlatformSenderClass();

    default SRCommandSender detectAndConvertSender(Object sender) {
        if (sender instanceof SRCommandSender) {
            return (SRCommandSender) sender;
        } else {
            return convertPlatformSender(getPlatformSenderClass().cast(sender));
        }
    }
}
