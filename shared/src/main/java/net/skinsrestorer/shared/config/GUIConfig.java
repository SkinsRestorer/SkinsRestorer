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

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newCappedProperty;

public class GUIConfig implements SettingsHolder {
    @Comment({
            "The placeholder skin for locked entries in the /skins GUI.",
            "Enter the final part of the skin texture URL.",
            "Use /sr info skin <skinName> to get the texture URL."
    })
    public static final Property<String> NOT_UNLOCKED_SKIN = newProperty("gui.notUnlockedSkin", "c10591e6909e6a281b371836e462d67a2c78fa0952e910f32b41a26c48c1757c");
    @Comment("Shows custom skins in the /skins GUI.")
    public static final Property<Boolean> CUSTOM_GUI_ENABLED = newProperty("gui.custom.enabled", true);
    @Comment("Sets the position of custom skins relative to other skin types.")
    public static final Property<Integer> CUSTOM_GUI_INDEX = newCappedProperty("gui.custom.index", 0, 0, Integer.MAX_VALUE);
    @Comment("Shows only the custom skins in gui.custom.list.")
    public static final Property<Boolean> CUSTOM_GUI_ONLY_LIST = newProperty("gui.custom.onlyShowList", false);
    @Comment("The custom skins to show when gui.custom.onlyShowList is enabled.")
    public static final Property<List<String>> CUSTOM_GUI_LIST = newListProperty("gui.custom.list", "xknat", "pistonmaster");
    @Comment("Shows player skins in the /skins GUI.")
    public static final Property<Boolean> PLAYERS_GUI_ENABLED = newProperty("gui.players.enabled", false);
    @Comment("Sets the position of player skins relative to other skin types.")
    public static final Property<Integer> PLAYERS_GUI_INDEX = newCappedProperty("gui.players.index", 1, 0, Integer.MAX_VALUE);
    @Comment("Shows only the player skins in gui.players.list.")
    public static final Property<Boolean> PLAYERS_GUI_ONLY_LIST = newProperty("gui.players.onlyShowList", false);
    @Comment("The player skins to show when gui.players.onlyShowList is enabled.")
    public static final Property<List<String>> PLAYERS_GUI_LIST = newListProperty("gui.players.list", "7dcfc130-344a-4719-9fbe-3176bc2075c6", "b1ae0778-4817-436c-96a3-a72c67cda060");
    @Comment("Shows recommended skins in the /skins GUI.")
    public static final Property<Boolean> RECOMMENDATIONS_GUI_ENABLED = newProperty("gui.recommendations.enabled", true);
    @Comment("Sets the position of recommended skins relative to other skin types.")
    public static final Property<Integer> RECOMMENDATIONS_GUI_INDEX = newCappedProperty("gui.recommendations.index", 2, 0, Integer.MAX_VALUE);
    @Comment("Shows only the recommended skins in gui.recommendations.list.")
    public static final Property<Boolean> RECOMMENDATIONS_GUI_ONLY_LIST = newProperty("gui.recommendations.onlyShowList", false);
    @Comment("The recommended skins to show when gui.recommendations.onlyShowList is enabled.")
    public static final Property<List<String>> RECOMMENDATIONS_GUI_LIST = newListProperty("gui.recommendations.list", "vampire", "space-suit");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("gui",
                "\n",
                "Controls the skins that appear in the /skins GUI."
        );
        conf.setComment("gui.custom",
                "Controls custom skins in the /skins GUI."
        );
        conf.setComment("gui.players",
                "Controls player skins in the /skins GUI."
        );
        conf.setComment("gui.recommendations",
                "Controls recommended skins in the /skins GUI."
        );
    }
}
