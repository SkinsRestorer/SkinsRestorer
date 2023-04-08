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
package net.skinsrestorer.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.commands.library.CommandExecutor;
import net.skinsrestorer.shared.commands.library.CommandUtils;
import net.skinsrestorer.shared.commands.library.SRRegisterPayload;
import net.skinsrestorer.shared.subjects.SRCommandSender;

public class SRBungeeCommand extends Command implements TabExecutor {
    private final CommandExecutor<SRCommandSender> executor;
    private final WrapperBungee wrapper;

    public SRBungeeCommand(SRRegisterPayload<SRCommandSender> payload, WrapperBungee wrapper) {
        super(payload.getMeta().getRootName(), null, payload.getMeta().getAliases());
        this.executor = payload.getExecutor();
        this.wrapper = wrapper;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        executor.execute(wrapper.commandSender(sender), CommandUtils.joinCommand(getName(), args));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return executor.tabComplete(wrapper.commandSender(sender), CommandUtils.joinCommand(getName(), args)).join();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return executor.hasPermission(wrapper.commandSender(sender));
    }
}
