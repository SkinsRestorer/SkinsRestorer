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

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class DevConfig implements SettingsHolder {
    @Comment("Enable to start receiving debug messages about api requests & more.")
    public static final Property<Boolean> DEBUG = newProperty("dev.debug", false);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("dev",
                "\n",
                "Updater Settings",
                "<!! Warning !!>",
                "Using outdated version void's support, compatibility & stability.",
                "\n",
                "To block all types of automatic updates (which can risk keeping an exploit):",
                "Create a file called 'noupdate.txt' in the plugin directory (./plugins/SkinsRestorer/ )",
                "\n",
                "\n################",
                "\n# DEV's corner #",
                "\n################",
                "\n",
                "Enable these on the dev's request"
        );
    }
}
