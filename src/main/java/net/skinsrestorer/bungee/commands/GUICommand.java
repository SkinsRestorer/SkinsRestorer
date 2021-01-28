/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.storage.Locale;

@CommandAlias("skins")
@CommandPermission("%skins")
public class GUICommand extends BaseCommand {
    private final SkinsRestorer plugin;

    public GUICommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage(TextComponent.fromLegacyText("SkinsRestorer Help"));
        help.showHelp();
    }

    @Default
    @CommandPermission("%skins")
    public void onDefault(ProxiedPlayer p) {
        p.sendMessage(TextComponent.fromLegacyText(Locale.SKINSMENU_OPEN));

        plugin.getPluginMessageListener().sendGuiOpenRequest(p);
    }
}
