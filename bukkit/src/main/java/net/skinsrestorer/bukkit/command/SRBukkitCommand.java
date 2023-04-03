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
package net.skinsrestorer.bukkit.command;

import lombok.Getter;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.commands.library.CommandExecutor;
import net.skinsrestorer.shared.commands.library.CommandUtils;
import net.skinsrestorer.shared.commands.library.PlatformRegistration;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SRBukkitCommand extends Command implements PluginIdentifiableCommand {
    @Getter
    private final Plugin plugin;
    private final CommandExecutor<SRCommandSender> executor;
    private final WrapperBukkit wrapper;

    public SRBukkitCommand(PlatformRegistration<SRCommandSender> registration, Plugin plugin, WrapperBukkit wrapper) {
        super(registration.getRootNode());
        this.plugin = plugin;
        this.executor = registration.getExecutor();
        this.wrapper = wrapper;
        setAliases(Arrays.asList(registration.getAliases()));
        setPermission(registration.getRootPermission());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        executor.execute(wrapper.commandSender(sender), CommandUtils.joinCommand(commandLabel, args));
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return executor.tabComplete(wrapper.commandSender(sender), CommandUtils.joinCommand(alias, args)).join();
    }
}
