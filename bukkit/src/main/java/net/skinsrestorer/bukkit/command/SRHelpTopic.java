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
package net.skinsrestorer.bukkit.command;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import org.bukkit.command.CommandSender;
import org.bukkit.help.HelpTopic;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class SRHelpTopic extends HelpTopic {
    private final SRBukkitCommand srbukkitCommand;
    private final WrapperBukkit wrapper;
    private final SkinsRestorerLocale locale;

    @Override
    public boolean canSee(@NotNull CommandSender player) {
        return srbukkitCommand.testPermissionSilent(player);
    }

    @NotNull
    @Override
    public String getName() {
        return "/" + srbukkitCommand.getMeta().getRootName();
    }

    @NotNull
    @Override
    public String getShortText() {
        return locale.getMessage(locale.getDefaultForeign(),
                srbukkitCommand.getMeta().getRootHelp().getCommandDescription());
    }

    @NotNull
    @Override
    public String getFullText(@NotNull CommandSender forWho) {
        SRCommandSender sender = wrapper.commandSender(forWho);
        return String.join("\n",
                srbukkitCommand.getExecutor().getManager().getHelpMessage(srbukkitCommand.getMeta().getRootName(), sender));
    }
}
