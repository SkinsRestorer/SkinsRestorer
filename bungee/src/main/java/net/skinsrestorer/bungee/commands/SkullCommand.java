package net.skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.commands.IProxySkullCommand;

import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapCommandSender;
import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapPlayer;

@Getter
@RequiredArgsConstructor
@CommandAlias("skull")
@CommandPermission("%skull")
public class SkullCommand extends BaseCommand implements IProxySkullCommand {
    private final SkinsRestorer plugin;

    @HelpCommand
    public void onHelp(CommandSender sender, CommandHelp help) {
        onHelp(wrapCommandSender(sender), help);
    }

    @Default
    @CommandPermission("%skull")
    public void onDefault(ProxiedPlayer player) {
        onDefault(wrapPlayer(player));
    }
}
