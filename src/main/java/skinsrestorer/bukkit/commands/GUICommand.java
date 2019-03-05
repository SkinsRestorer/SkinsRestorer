package skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsGUI;
import skinsrestorer.shared.storage.Locale;


@CommandAlias("skins") @CommandPermission("%skins")
public class GUICommand extends BaseCommand {
    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    @Default @CommandPermission("%skins")
    public void onDefault(Player p) {
        SkinsGUI.getMenus().put(p.getName(), 0);
        p.openInventory(SkinsGUI.getGUI(0));
        p.sendMessage(Locale.MENU_OPEN);
    }
}