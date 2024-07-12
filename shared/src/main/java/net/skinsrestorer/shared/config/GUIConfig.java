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
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class GUIConfig implements SettingsHolder {
    public static final Property<Boolean> CUSTOM_GUI_ENABLED = newProperty("gui.custom.enabled", true);
    public static final Property<Integer> CUSTOM_GUI_INDEX = newProperty("gui.custom.index", 0);
    public static final Property<Boolean> CUSTOM_GUI_ONLY_LIST = newProperty("gui.custom.onlyShowList", false);
    public static final Property<List<String>> CUSTOM_GUI_LIST = newListProperty("gui.custom.list", "xknat", "pistonmaster");
    public static final Property<Boolean> PLAYERS_GUI_ENABLED = newProperty("gui.players.enabled", false);
    public static final Property<Integer> PLAYERS_GUI_INDEX = newProperty("gui.players.index", 1);
    public static final Property<Boolean> PLAYERS_GUI_ONLY_LIST = newProperty("gui.players.onlyShowList", false);
    public static final Property<List<String>> PLAYERS_GUI_LIST = newListProperty("gui.players.list", "7dcfc130-344a-4719-9fbe-3176bc2075c6", "b1ae0778-4817-436c-96a3-a72c67cda060");
    public static final Property<Boolean> RECOMMENDATIONS_GUI_ENABLED = newProperty("gui.recommendations.enabled", true);
    public static final Property<Integer> RECOMMENDATIONS_GUI_INDEX = newProperty("gui.recommendations.index", 2);
    public static final Property<Boolean> RECOMMENDATIONS_GUI_ONLY_LIST = newProperty("gui.recommendations.onlyShowList", false);
    public static final Property<List<String>> RECOMMENDATIONS_GUI_LIST = newListProperty("gui.recommendations.list", "vampire", "space-suit");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("gui",
                "\n",
                "Control what skins appear in the /skins GUI"
        );
    }
}
