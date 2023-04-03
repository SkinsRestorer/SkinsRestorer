package net.skinsrestorer.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.commands.library.CommandExecutor;
import net.skinsrestorer.shared.commands.library.PlatformRegistration;
import net.skinsrestorer.shared.subjects.SRCommandSender;

public class SRBungeeCommand extends Command implements TabExecutor {
    private final CommandExecutor<SRCommandSender> executor;
    private final WrapperBungee wrapper;

    public SRBungeeCommand(PlatformRegistration<SRCommandSender> registration, WrapperBungee wrapper) {
        super(registration.getRootNode(), registration.getRootPermission(), registration.getAliases());
        this.executor = registration.getExecutor();
        this.wrapper = wrapper;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        executor.execute(wrapper.commandSender(sender), getName() + " " + String.join(" ", args));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return executor.tabComplete(wrapper.commandSender(sender), getName() + " " + String.join(" ", args)).join();
    }
}
