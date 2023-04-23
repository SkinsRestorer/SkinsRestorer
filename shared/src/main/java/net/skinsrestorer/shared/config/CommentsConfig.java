/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;

public class CommentsConfig implements SettingsHolder {
    private CommentsConfig() {
    }

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("",
                "\n##################################",
                "\n#      SkinsRestorer config      #",
                "\n##################################",
                "\n",
                "We from SRTeam thank you for using our plugin!",
                "For more information        -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/",
                "For installation            -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Installing-SkinsRestorer",
                "For Configuration Help      -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Configuration",
                "Commands & Permissions      -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/cmds-&-perms",
                "For locale & messages       -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Locale-and-Translations",
                "Not working or get error?  -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Troubleshoot",
                "For advanced help or other, go to our Discord: https://discord.me/SkinsRestorer/",
                "\n",
                "(?) Step by step config guide -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Configuration",
                "\n",
                "(!) IF YOU ARE USING A PROXY (Bungee, Waterfall or Velocity), Check & set on every BACKEND server spigot.yml -> bungeecord: true.  (!)",
                "(!) & Install Skinsrestorer.jar on ALL SERVERS!!! (BOTH Backend & Proxy).                      (!)"
        );
        conf.setFooter(
                "\n",
                "\n# End #",
                "\n",
                "Useful tools:",
                "Vectier Thailand has made some super cool \"Custom Skin\" tools that you can use!",
                "",
                "SkinFile Generator:",
                "With SkinFile Generator, you can upload your own custom skin to get a unique .skin file that you can put in your skins folder, to use with SkinsRestorer.",
                "Check it out here: https://skinsrestorer.github.io/SkinFile-Generator/",
                "",
                "SkinSystem :",
                "With SkinSystem, you, as a server owner, can connect AuthMe (and forum) with the SkinSystem website that you can host, to give your players the ability to upload custom skins.",
                "Check it out here: https://github.com/SkinsRestorer/SkinSystem",
                "",
                "\n# Useful Links #",
                "Website: https://skinsrestorer.net/",
                "Download: https://github.com/SkinsRestorer/SkinsRestorerX/releases",
                "Wiki https://github.com/SkinsRestorer/SkinsRestorerX/wiki/",
                "Spigot: https://www.spigotmc.org/resources/skinsrestorer.2124/",
                "Github: https://github.com/SkinsRestorer/SkinsRestorerX/",
                "Discord: https://discord.me/SkinsRestorer/"
        );
    }
}
