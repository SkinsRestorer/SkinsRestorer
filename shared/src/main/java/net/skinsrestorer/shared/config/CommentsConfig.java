/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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

public final class CommentsConfig implements SettingsHolder {
    private CommentsConfig() {
    }

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("",
                "\n##################################",
                "\n#      SkinsRestorer config      #",
                "\n##################################",
                "\n",
                "Thank you for using SkinsRestorer.",
                "\n",
                "Read the installation guide before you request support:",
                "https://skinsrestorer.net/docs/installation",
                "\n",
                "Configuration guides:",
                "General configuration: https://skinsrestorer.net/docs/configuration",
                "Commands and permissions: https://skinsrestorer.net/docs/configuration/commands-permissions",
                "Translations and messages: https://skinsrestorer.net/docs/configuration/locale-translations",
                "\n",
                "If you have a problem, read the troubleshooting guide:",
                "https://skinsrestorer.net/docs/troubleshooting",
                "For more support, join the Discord server: https://skinsrestorer.net/discord",
                "\n",
                "Installation guide: https://skinsrestorer.net/docs/installation",
                "\n",
                "WARNING: Proxy networks require SkinsRestorer on the proxy and every backend server.",
                "Configure every backend server to accept connections from the proxy.",
                "Use the same SkinsRestorer configuration on each proxy and backend server.",
                "Proxy installation guide: https://skinsrestorer.net/docs/installation"
        );
        conf.setFooter(
                "\n",
                "\n# End #",
                "\n",
                "Useful tools:",
                "",
                "SkinFile Generator:",
                "Upload a custom skin and download a .skin file for the SkinsRestorer skins folder.",
                "https://generator.skinsrestorer.net",
                "",
                "SkinSystem:",
                "Host a website that connects to AuthMe and lets players upload custom skins.",
                "https://github.com/SkinsRestorer/SkinSystem",
                "",
                "\n# Useful Links #",
                "Website: https://skinsrestorer.net",
                "Docs: https://skinsrestorer.net/docs",
                "Spigot: https://skinsrestorer.net/spigot",
                "GitHub: https://github.com/SkinsRestorer/SkinsRestorer",
                "Discord: https://skinsrestorer.net/discord"
        );
    }
}
