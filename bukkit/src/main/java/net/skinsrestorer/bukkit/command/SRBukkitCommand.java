package net.skinsrestorer.bukkit.command;

import lombok.Getter;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.commands.library.CommandExecutor;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SRBukkitCommand extends Command implements PluginIdentifiableCommand {
    @Getter
    private final Plugin plugin;
    private final CommandExecutor<SRCommandSender> executor;
    private final WrapperBukkit wrapper;

    public SRBukkitCommand(@NotNull String name, Plugin plugin, CommandExecutor<SRCommandSender> executor, WrapperBukkit wrapper) {
        super(name);
        this.plugin = plugin;
        this.executor = executor;
        this.wrapper = wrapper;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        executor.execute(wrapper.commandSender(sender), commandLabel + " " + String.join(" ", args));
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return super.tabComplete(sender, alias, args);
    }
}
