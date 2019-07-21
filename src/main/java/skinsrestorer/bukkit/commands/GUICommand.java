package skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import skinsrestorer.bukkit.SkinsGUI;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Locale;


@CommandAlias("skins") @CommandPermission("%skins")
public class GUICommand extends BaseCommand {
    private SkinsRestorer plugin;
    private SkinsGUI skinsGUI;

    public GUICommand(SkinsRestorer plugin) {
        this.plugin = plugin;
        this.skinsGUI = new SkinsGUI(this.plugin);
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    @Default @CommandPermission("%skins")
    public void onDefault(Player p) {
        p.sendMessage(Locale.MENU_OPEN);

        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            SkinsGUI.getMenus().put(p.getName(), 0);
            Inventory inventory = this.skinsGUI.getGUI(p, 0);
            Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                p.openInventory(inventory);
            });
        });
    }
}