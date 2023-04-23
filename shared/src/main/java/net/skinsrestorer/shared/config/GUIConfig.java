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
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class GUIConfig implements SettingsHolder {
    public static final Property<Boolean> CUSTOM_GUI_ENABLED = newProperty("customGUI.enabled", false);
    public static final Property<Boolean> CUSTOM_GUI_ONLY = newProperty("customGUI.showOnlyCustomGUI", true);
    public static final Property<List<String>> CUSTOM_GUI_SKINS = newListProperty("customGUI.list", "xknat", "pistonmaster");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("customGUI",
                "Custom list for the /skins GUI.",
                "ShowOnlyCustomGUI will only show CustomGUI.Names in the gui."
        );
    }
}
