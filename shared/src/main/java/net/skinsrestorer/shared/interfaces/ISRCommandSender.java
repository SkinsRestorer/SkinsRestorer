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
package net.skinsrestorer.shared.interfaces;

import net.skinsrestorer.shared.SkinsRestorerAPIShared;

public interface ISRCommandSender extends ISRForeign {
    void sendMessage(String message);

    default void sendMessage(MessageKeyGetter key, Object... args) {
        sendMessage(SkinsRestorerAPIShared.getApi().getMessage(this, key, args));
    }

    String getName();

    boolean hasPermission(String permission);

    boolean isConsole();

    default boolean equalsPlayer(ISRPlayer player) {
        return getName().equals(player.getName());
    }
}
