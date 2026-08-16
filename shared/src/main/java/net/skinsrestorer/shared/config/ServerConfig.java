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

public class ServerConfig implements SettingsHolder {
    @Comment({
            "Reapplies a player's skin after a server resource pack loads or fails.",
            "Resource-pack changes can reset player skins. Disable this option only if another plugin handles the reset."
    })
    // TODO: Should this not be in LoginConfig?
    public static final Property<Boolean> RESOURCE_PACK_FIX = newProperty("server.resourcePackFix", true);
    @Comment({
            "Dismounts a mounted or seated player before a skin update.",
            "This action prevents synchronization problems."
    })
    public static final Property<Boolean> DISMOUNT_PLAYER_ON_UPDATE = newProperty("server.dismountPlayerOnSkinUpdate", true);
    @Comment({
            "Remounts a player after the skin update.",
            "This option requires server.dismountPlayerOnSkinUpdate.",
            "Disable it only if a ride or sit plugin manages the player's mount."
    })
    public static final Property<Boolean> REMOUNT_PLAYER_ON_UPDATE = newProperty("server.remountPlayerOnSkinUpdate", true);
    @Comment({
            "Dismounts all passengers from a player before a skin update.",
            "This action prevents synchronization problems in ride plugins."
    })
    public static final Property<Boolean> DISMOUNT_PASSENGERS_ON_UPDATE = newProperty("server.dismountPassengersOnSkinUpdate", false);
    @Comment({
            "Plays a sound when /skin changes a player's skin."
    })
    public static final Property<Boolean> SOUND_ENABLED = newProperty("server.sound.enabled", true);
    @Comment({
            "The sound that plays after /skin changes a player's skin.",
            "Sound format and values:",
            "https://javadoc.io/static/com.github.cryptomorin/XSeries/11.0.0/com/cryptomorin/xseries/XSound.html#parse(java.lang.String)"
    })
    public static final Property<String> SOUND_VALUE = newProperty("server.sound.value", "ENTITY_PLAYER_TELEPORT, 0.7");
    @Comment({
            "Controls proxy mode. Valid values are ENABLED, DISABLED, and AUTO.",
            "AUTO detects proxy mode from the server configuration."
    })
    public static final Property<ProxyMode> PROXY_MODE_DETECTION = newProperty(ProxyMode.class, "server.proxyMode.detection", ProxyMode.AUTO);
    @Comment({
            "Enables server-side calls to the SkinsRestorer API in proxy mode.",
            "This option requires database storage. It does not work with file storage."
    })
    public static final Property<Boolean> PROXY_MODE_API = newProperty("server.proxyMode.api", true);
    @Comment({
            "Enables support for the SkinShuffle mod.",
            "SkinShuffle clients can change skins without reconnecting.",
            "On proxy networks, install SkinsRestorer on the proxy and every backend server."
    })
    public static final Property<Boolean> SKINSHUFFLE_SUPPORT = newProperty("server.skinShuffleSupport", true);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("server",
                "\n",
                "\n##########",
                "\n# Server #",
                "\n##########",
                "\n",
                "Controls server behavior."
        );
        conf.setComment("server.proxyMode",
                "Controls how SkinsRestorer works with proxies."
        );
    }

    public enum ProxyMode {
        ENABLED,
        DISABLED,
        AUTO
    }
}
