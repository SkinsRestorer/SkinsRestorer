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
    public static final Property<Boolean> RESOURCE_PACK_FIX = newProperty("server.resourcePackFix", true);
    @Comment({
            "Dismounts a mounted (on a horse, or sitting) player when their skin is updated, preventing players from becoming desynced.",
            "File override = ./plugins/SkinsRestorer/disableDismountPlayer.txt"
    })
    public static final Property<Boolean> DISMOUNT_PLAYER_ON_UPDATE = newProperty("server.dismountPlayerOnSkinUpdate", true);
    @Comment({
            "Remounts a player that was dismounted after a skin update (above option must be true).",
            "Disabling this is only recommended if you use plugins that allow you ride other players, or use sit. Otherwise you could get errors or players could be kicked for flying.",
            "File override = ./plugins/SkinsRestorer/disableRemountPlayer.txt"
    })
    public static final Property<Boolean> REMOUNT_PLAYER_ON_UPDATE = newProperty("server.remountPlayerOnSkinUpdate", true);
    @Comment({
            "Dismounts all passengers mounting a player (such as plugins that let you ride another player), preventing those players from becoming desynced.",
            "File override = ./plugins/SkinsRestorer/enableDismountEntities.txt"
    })
    public static final Property<Boolean> DISMOUNT_PASSENGERS_ON_UPDATE = newProperty("server.dismountPassengersOnSkinUpdate", false);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("server",
                "\n##########",
                "\n# Server #",
                "\n##########",
                "\n",
                "If we break things, you can disable it here."
        );
    }
}
