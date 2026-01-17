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
import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;
import net.kyori.adventure.text.Component;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class AdvancedConfig implements SettingsHolder {
    @Comment({
            "<!! Warning !!>",
            "Enabling this will stop SkinsRestorer to change skins on join.",
            "Handy for when you want run /skin apply to apply skin after texturepack popup"
    })
    public static final Property<Boolean> DISABLE_ON_JOIN_SKINS = newProperty("advanced.disableOnJoinSkins", false);
    @Comment({
            "<!! Warning !!>",
            "This enables the PaperMC join event integration that allows instant skins on join.",
            "It is recommended over the fallback listener because it is smoother and should not lag the server.",
            "It also fixes all resource pack skin apply issues.",
            "If your players are experiencing extremely long loading screens, try disabling this."
    })
    public static final Property<Boolean> ENABLE_PAPER_JOIN_LISTENER = newProperty("advanced.enablePaperJoinListener", true);
    @Comment({
            "<!! Warning !!>",
            "This is a very dangerous feature that should only be used if you know what you are doing.",
            "Instead of resending player skins to other players by hiding and shadowing them, we will teleport them to a far away location and back",
            "This is a workaround for some server software with broken vanishing support, like Arclight.",
    })
    public static final Property<Boolean> TELEPORT_REFRESH = newProperty("advanced.teleportRefresh", false);
    @Comment({
            "<!! Warning !!>",
            "When enabled SkinsRestorer will not try to connect to any web server, which means the follow things won't work:",
            "Getting new skins from Mojang, looking up uuids of players, skin url, update checking and more.",
            "SkinsRestorer will only be able to access already downloaded skins.",
            "This is useful for servers that are not connected to the internet or have a firewall blocking connections."
    })
    public static final Property<Boolean> NO_CONNECTIONS = newProperty("advanced.noConnections", false);
    @Comment({
            "<!! Warning !!>",
            "When enabled SkinsRestorer will use the new player head objects inside chat messages.",
            "While these new objects look great, they are not supported by all server software and plugins, especially older servers.",
            "This option will be on by default in the future when backwards compatibility is no longer a concern.",
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
                "ABSOLUTELY DO NOT CHANGE SETTINGS HERE IF YOU DO NOT KNOW WHAT YOU DO!"
        );
    }
}
