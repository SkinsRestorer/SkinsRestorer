/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
                "\n",
                "We recommend you read the official installation guide before asking for help:",
                "https://skinsrestorer.net/docs/installation",
                "\n",
                "We have prepared a few resources to help you configure the plugin:",
                "General help: https://skinsrestorer.net/docs/configuration",
                "Commands and permissions: https://skinsrestorer.net/docs/configuration/commands-permissions",
                "Translations and messages: https://skinsrestorer.net/docs/configuration/locale-translations",
                "\n",
                "If you encounter issues, you can do the following:",
                "Read the troubleshooting guide: https://skinsrestorer.net/docs/troubleshooting",
                "For advanced help or other, go to our Discord Server: https://skinsrestorer.net/discord",
                "\n",
                "(?) Step by step installation guide: https://skinsrestorer.net/docs/installation",
                "\n",
                "(!) IF YOU ARE USING A PROXY (BungeeCord, Waterfall or Velocity), YOU NEED TO INSTALL SKINSRESTORER ON THE PROXY AND ALL SERVERS! (!)",
                "(!) YOU ALSO NEED TO CONFIGURE YOUR SERVERS TO DETECT THE PROXY! (!)",
                "(!) AND YOU ALSO NEED TO PUT THE SAME CONFIG FILE IN ALL PROXIES AND SERVERS! (!)",
                "(!) You can find detailed proxy instructions here: https://skinsrestorer.net/docs/installation"
        );
        conf.setFooter(
                "\n",
                "\n# End #",
                "\n",
                "Useful tools:",
                "",
                "SkinFile Generator:",
                "With SkinFile Generator, you can upload your own custom skin to get a unique .skin file that you can put in your skins folder, to use with SkinsRestorer.",
                "Check it out here: https://generator.skinsrestorer.net",
                "",
                "SkinSystem:",
                "With SkinSystem, you, as a server owner, can connect AuthMe with the SkinSystem website that you can host, to give your players the ability to upload custom skins.",
                "Check it out here: https://github.com/SkinsRestorer/SkinSystem",
                "",
                "\n# Useful Links #",
                "Website: https://skinsrestorer.net",
                "Docs: https://skinsrestorer.net/docs",
                "Spigot: https://skinsrestorer.net/spigot",
                "Github: https://github.com/SkinsRestorer/SkinsRestorer",
                "Discord: https://skinsrestorer.net/discord"
        );
    }
}
