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
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Locale;

public interface ISkinCommand {
    default void sendHelp(ISRCommandSender sender) {
        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);

        sender.sendMessage(Locale.CUSTOM_HELP_IF_ENABLED.replace("%ver%", getPlugin().getVersion()));

        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);
    }

    default void rollback(String pName, String oldSkinName, boolean save) {
        if (save)
            getPlugin().getSkinStorage().setSkinName(pName, oldSkinName);
    }

    ISRPlugin getPlugin();

    enum SkinType {
        STEVE,
        SLIM,
    }
}
