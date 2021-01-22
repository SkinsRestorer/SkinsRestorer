/*
 *     This file is part of SkinsRestorer.
 *
 *     SkinsRestorer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SkinsRestorer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SkinsRestorer. If not, see <http://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import net.skinsrestorer.bukkit.SkinsGUI;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.storage.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@CommandAlias("skins")
@CommandPermission("%skins")
public class GUICommand extends BaseCommand {
    private final SkinsGUI skinsGUI;

    public GUICommand(SkinsRestorer plugin) {
        this.skinsGUI = new SkinsGUI(plugin);
    }

    //todo is help even needed for /skins?
    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    @Default
    @CommandPermission("%skins")
    public void onDefault(Player p) {
        p.sendMessage(Locale.SKINSMENU_OPEN);

        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            SkinsGUI.getMenus().put(p.getName(), 0);
            Inventory inventory = this.skinsGUI.getGUI(p, 0);
            Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                p.openInventory(inventory);
            });
        });
    }
}