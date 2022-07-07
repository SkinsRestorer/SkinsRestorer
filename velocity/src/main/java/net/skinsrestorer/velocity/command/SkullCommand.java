package net.skinsrestorer.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.IProxySkullCommand;
import net.skinsrestorer.velocity.SkinsRestorer;

import static net.skinsrestorer.velocity.utils.WrapperVelocity.wrapCommandSender;
import static net.skinsrestorer.velocity.utils.WrapperVelocity.wrapPlayer;

@Getter
@RequiredArgsConstructor
@CommandAlias("skull")
@CommandPermission("%skull")
public class SkullCommand extends BaseCommand implements IProxySkullCommand {
    private final SkinsRestorer plugin;

    @HelpCommand
    public void onHelp(CommandSource sender, CommandHelp help) {
        onHelp(wrapCommandSender(sender), help);
    }

    @Default
    @CommandPermission("%skull")
    public void onDefault(Player player) {
        onDefault(wrapPlayer(player));
    }
}
