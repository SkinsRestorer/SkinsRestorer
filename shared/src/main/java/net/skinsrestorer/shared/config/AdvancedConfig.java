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
import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;
import net.kyori.adventure.text.Component;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class AdvancedConfig implements SettingsHolder {
    @Comment({
            "WARNING:",
            "When enabled, SkinsRestorer does not apply skins when players join.",
            "Enable this only if another plugin runs /skin apply after the resource-pack prompt."
    })
    public static final Property<Boolean> DISABLE_ON_JOIN_SKINS = newProperty("advanced.disableOnJoinSkins", false);
    @Comment({
            "Enables the Paper join event integration for immediate skins when players join.",
            "This integration also reapplies skins after a resource pack loads.",
            "If players have long loading screens, disable this option."
    })
    public static final Property<Boolean> ENABLE_PAPER_JOIN_LISTENER = newProperty("advanced.enablePaperJoinListener", true);
    @Comment({
            "CAUTION:",
            "This option teleports players to a distant location and back during a skin refresh.",
            "Use this workaround only when broken vanish support prevents normal skin refreshes.",
            "Arclight is one platform that can require this workaround."
    })
    public static final Property<Boolean> TELEPORT_REFRESH = newProperty("advanced.teleportRefresh", false);
    @Comment({
            "WARNING:",
            "When enabled, SkinsRestorer does not connect to external services.",
            "Mojang skins, player UUID lookups, skin URLs, and update checks will not work.",
            "SkinsRestorer will only be able to access already downloaded skins.",
            "Enable this option only on servers without internet access."
    })
    public static final Property<Boolean> NO_CONNECTIONS = newProperty("advanced.noConnections", false);
    @Comment({
            "When enabled, SkinsRestorer uses player-head components in chat messages.",
            "Some server platforms and plugins do not support these components.",
            "This limitation is more common on older servers."
    })
    public static final Property<Boolean> PLAYER_HEAD_CHAT_OBJECTS = newProperty("advanced.playerHeadChatObjects", false);

    public static Component emptyIfPlayerHeadChatObjectsDisabled(SettingsManager settingsManager, Component component) {
        if (settingsManager.getProperty(PLAYER_HEAD_CHAT_OBJECTS)) {
            return component;
        }

        return Component.empty();
    }

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("advanced",
                "\n",
                "\n###############",
                "\n# Danger Zone #",
                "\n###############",
                "\n",
                "CAUTION: These options change advanced behavior. Change them only to solve a specific problem."
        );
    }
}
