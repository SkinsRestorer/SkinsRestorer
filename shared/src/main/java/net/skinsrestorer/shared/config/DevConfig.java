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

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class DevConfig implements SettingsHolder {
    @Comment("Enables debug messages for API requests and other plugin activity.")
    public static final Property<Boolean> DEBUG = newProperty("dev.debug", false);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("dev",
                "\n",
                "Update settings",
                "WARNING: Outdated versions do not receive support and can contain known security problems.",
                "\n",
                "To disable all automatic updates, create this file:",
                "./plugins/SkinsRestorer/noautoupdate.txt",
                "\n",
                "\n################",
                "\n# Developer options #",
                "\n################",
                "\n",
                "Enable these options only when a SkinsRestorer developer asks you to."
        );
    }
}
