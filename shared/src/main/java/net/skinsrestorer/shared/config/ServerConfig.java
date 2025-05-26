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

public class ServerConfig implements SettingsHolder {
    @Comment({
            "Disabling this will stop SkinsRestorer from changing skins when a player loads a server resource pack.",
            "When a player loads a server resource pack, their skin is reset. By default, SkinsRestorer reapplies the skin when the player reports that the resource pack has been loaded or an error has occurred."
    })
    // TODO: Should this not be in LoginConfig?
    public static final Property<Boolean> RESOURCE_PACK_FIX = newProperty("server.resourcePackFix", true);
    @Comment({
            "Dismounts a mounted (on a horse, or sitting) player when their skin is updated, preventing players from becoming desynced.",
    })
    public static final Property<Boolean> DISMOUNT_PLAYER_ON_UPDATE = newProperty("server.dismountPlayerOnSkinUpdate", true);
    @Comment({
            "Remounts a player that was dismounted after a skin update (above option must be true).",
            "Disabling this is only recommended if you use plugins that allow you ride other players, or use sit. Otherwise you could get errors or players could be kicked for flying.",
    })
    public static final Property<Boolean> REMOUNT_PLAYER_ON_UPDATE = newProperty("server.remountPlayerOnSkinUpdate", true);
    @Comment({
            "Dismounts all passengers mounting a player (such as plugins that let you ride another player), preventing those players from becoming desynced.",
    })
    public static final Property<Boolean> DISMOUNT_PASSENGERS_ON_UPDATE = newProperty("server.dismountPassengersOnSkinUpdate", false);
    @Comment({
            "Play a sound when a player runs /skin to change their skin."
    })
    public static final Property<Boolean> SOUND_ENABLED = newProperty("server.sound.enabled", true);
    @Comment({
            "Sound to play when a player runs /skin to change their skin.",
            "You can find the allowed format and values at",
            "https://javadoc.io/static/com.github.cryptomorin/XSeries/11.0.0/com/cryptomorin/xseries/XSound.html#parse(java.lang.String)"
    })
    public static final Property<String> SOUND_VALUE = newProperty("server.sound.value", "ENTITY_PLAYER_TELEPORT, 0.7");
    @Comment({
            "Proxy mode detection. Valid values are ENABLED, DISABLED, AUTO. Auto will auto detect based on your server configuration.",
    })
    public static final Property<ProxyMode> PROXY_MODE_DETECTION = newProperty(ProxyMode.class, "server.proxyMode.detection", ProxyMode.AUTO);
    @Comment({
            "Proxy mode API will make server-side plugin calls to SkinsRestorer API possible. Only works if a database is set up instead of file storage.",
    })
    public static final Property<Boolean> PROXY_MODE_API = newProperty("server.proxyMode.api", true);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("server",
                "\n",
                "\n##########",
                "\n# Server #",
                "\n##########",
                "\n",
                "Change server specific settings here."
        );
        conf.setComment("server.proxyMode",
                "Proxy mode settings. Allows SkinsRestorer to work with proxies."
        );
    }

    public enum ProxyMode {
        ENABLED,
        DISABLED,
        AUTO
    }
}
