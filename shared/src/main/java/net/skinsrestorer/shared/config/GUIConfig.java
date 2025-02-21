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

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newCappedProperty;

public class GUIConfig implements SettingsHolder {
    @Comment({
            "Control what skin is displayed when a player does not have permission for a skin.",
            "This is the end part of the skin texture URL.",
            "You can obtain the texture URL from /sr info skin <skinName>"
    })
    public static final Property<String> NOT_UNLOCKED_SKIN = newProperty("gui.notUnlockedSkin", "c10591e6909e6a281b371836e462d67a2c78fa0952e910f32b41a26c48c1757c");
    @Comment("Whether custom skins are enabled in the /skins GUI")
    public static final Property<Boolean> CUSTOM_GUI_ENABLED = newProperty("gui.custom.enabled", true);
    @Comment("Order of custom skins relative to the other skin types")
    public static final Property<Integer> CUSTOM_GUI_INDEX = newCappedProperty("gui.custom.index", 0, 0, Integer.MAX_VALUE);
    @Comment("Whether only specific custom skins are allowed in the /skins GUI")
    public static final Property<Boolean> CUSTOM_GUI_ONLY_LIST = newProperty("gui.custom.onlyShowList", false);
    @Comment("Specific custom skins to show in the /skins GUI")
    public static final Property<List<String>> CUSTOM_GUI_LIST = newListProperty("gui.custom.list", "xknat", "pistonmaster");
    @Comment("Whether player skins are enabled in the /skins GUI")
    public static final Property<Boolean> PLAYERS_GUI_ENABLED = newProperty("gui.players.enabled", false);
    @Comment("Order of player skins relative to the other skin types")
    public static final Property<Integer> PLAYERS_GUI_INDEX = newCappedProperty("gui.players.index", 1, 0, Integer.MAX_VALUE);
    @Comment("Whether only specific player skins are allowed in the /skins GUI")
    public static final Property<Boolean> PLAYERS_GUI_ONLY_LIST = newProperty("gui.players.onlyShowList", false);
    @Comment("Specific player skins to show in the /skins GUI")
    public static final Property<List<String>> PLAYERS_GUI_LIST = newListProperty("gui.players.list", "7dcfc130-344a-4719-9fbe-3176bc2075c6", "b1ae0778-4817-436c-96a3-a72c67cda060");
    @Comment("Whether recommended skins are enabled in the /skins GUI")
    public static final Property<Boolean> RECOMMENDATIONS_GUI_ENABLED = newProperty("gui.recommendations.enabled", true);
    @Comment("Order of recommended skins relative to the other skin types")
    public static final Property<Integer> RECOMMENDATIONS_GUI_INDEX = newCappedProperty("gui.recommendations.index", 2, 0, Integer.MAX_VALUE);
    @Comment("Whether only specific recommended skins are allowed in the /skins GUI")
    public static final Property<Boolean> RECOMMENDATIONS_GUI_ONLY_LIST = newProperty("gui.recommendations.onlyShowList", false);
    @Comment("Specific recommended skins to show in the /skins GUI")
    public static final Property<List<String>> RECOMMENDATIONS_GUI_LIST = newListProperty("gui.recommendations.list", "vampire", "space-suit");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("gui",
                "\n",
                "Control what skins appear in the /skins GUI"
        );
        conf.setComment("gui.custom",
                "Control custom skins in the /skins GUI"
        );
        conf.setComment("gui.players",
                "Control player skins in the /skins GUI"
        );
        conf.setComment("gui.recommendations",
                "Control recommended skins in the /skins GUI"
        );
    }
}
