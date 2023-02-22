/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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

public class AdvancedConfig implements SettingsHolder {
    @Comment({
            "<!! Warning !!>",
            "Enabling this will stop SkinsRestorer to change skins on join.",
            "Handy for when you want run /skin apply to apply skin after texturepack popup"
    })
    public static final Property<Boolean> DISABLE_ON_JOIN_SKINS = newProperty("advanced.disableOnJoinSkins", false);
    @Comment({
            "<!! Warning Experimental !!>",
            "This enables the experimental PaperMC join event integration that allows instant skins on join.",
            "It is not as tested as the default implementation, but it is smoother and should not lag the server.",
            "It also fixes all resource pack skin apply issues.",
            "If your players are experiencing extremely long loading screens, try disabling this."
    })
    public static final Property<Boolean> ENABLE_PAPER_JOIN_LISTENER = newProperty("advanced.enablePaperJoinListener", true);
    @Comment({
            "<!! Warning !!>",
            "When enabled if a skin gets applied on the proxy, the new texture will be forwarded to the backend as well.",
            "This is optional sometimes as the backend may pick up the new one of the proxy.",
            "It is recommended though to **KEEP THIS ON** because it keeps the backend data in sync.",
            "This feature is required for solutions like RedisBungee and also fixes bugs in some cases."
    })
    public static final Property<Boolean> FORWARD_TEXTURES = newProperty("advanced.forwardTextures", true);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("advanced",
                "\n###############",
                "\n# Danger Zone #",
                "\n###############",
                "\n",
                "ABSOLUTELY DO NOT CHANGE IF YOU DO NOT KNOW WHAT YOU DO"
        );
    }
}
