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

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class CommandConfig implements SettingsHolder {

    @Comment({
            "For all player commands to work by default, you need to give players the permission 'skinsrestorer.player'.",
            "This option allows you to force the default permission (skinsrestorer.player) to be given to all players.",
            "Any permission lookup will be bypassed if this is set to true."
    })
    public static final Property<Boolean> FORCE_DEFAULT_PERMISSIONS = newProperty("commands.forceDefaultPermissions", true);
    @Comment({
            "Players cooldown in seconds when changing skins (set to 0 to disable).",
            "SkinErrorCooldown is used when an error or invalid url occurs.",
            "Can be bypassed with 'skinsrestorer.bypasscooldown'."
    })
    public static final Property<Integer> SKIN_CHANGE_COOLDOWN = newProperty("commands.skinChangeCooldown", 30);

    public static final Property<Integer> SKIN_ERROR_COOLDOWN = newProperty("commands.skinErrorCooldown", 5);
    @Comment({
            "True will make use of the 'CUSTOM_HELP_IF_ENABLED' & `SR_LINE` in messages.yml.",
            "This is more customizable, but at the cost of permission specific help."
    })
    public static final Property<Boolean> ENABLE_CUSTOM_HELP = newProperty("commands.enableCustomHelp", false);
    public static final Property<Boolean> RESTRICT_SKIN_URLS_ENABLED = newProperty("commands.restrictSkinUrls.enabled", false);
    public static final Property<List<String>> RESTRICT_SKIN_URLS_LIST = newListProperty("commands.restrictSkinUrls.list",
            "https://i.imgur.com",
            "http://i.imgur.com",
            "https://storage.googleapis.com",
            "http://storage.googleapis.com",
            "https://cdn.discordapp.com",
            "http://cdn.discordapp.com",
            "https://textures.minecraft.net",
            "http://textures.minecraft.net"
    );
    public static final Property<Boolean> DISABLED_SKINS_ENABLED = newProperty("commands.disabledSkins.enabled", false);
    public static final Property<List<String>> DISABLED_SKINS = newListProperty("commands.disabledSkins.list", "steve", "owner");
    @Comment({
            "Allows the usage of per-skin permission.",
            "Example: skinsrestorer.skin.xknat OR skinsrestorer.skin.Pistonmaster",
            "with \"skinsrestorer.ownskin\" players can run /skin set %playerusername%.",
            "[!] Only enable if you have set up permissions properly and know what you are doing."
    })
    public static final Property<Boolean> PER_SKIN_PERMISSIONS = newProperty("commands.perSkinPermissions", false);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("commands",
                "\n###########",
                "\n# Commands #",
                "\n###########",
                "\n",
                "Control behaviour of commands."
        );
        conf.setComment("commands.disabledSkins",
                "Skins in this list will be disabled, so users can't set them.",
                "Can be bypassed with 'skinsrestorer.bypassdisabled'."
        );
        conf.setComment("commands.restrictSkinUrls",
                "When enabled, only websites from the list below is allowed to be set using /skin url <url>",
                "[?] this is useful if you host your own image server."
        );
    }
}
