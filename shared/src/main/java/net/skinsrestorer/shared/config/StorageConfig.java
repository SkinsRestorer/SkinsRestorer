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
import ch.jalu.configme.properties.ListProperty;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.utils.FluentList.listOf;

public class StorageConfig implements SettingsHolder {
    public static final Property<Boolean> DEFAULT_SKINS_ENABLED = newProperty("storage.defaultSkins.enabled", false);
    public static final Property<Boolean> DEFAULT_SKINS_PREMIUM = newProperty("storage.defaultSkins.applyForPremium", false);
    public static final Property<List<String>> DEFAULT_SKINS = new ListProperty<>("storage.defaultSkins.list", new PropertyType<String>() {
        @Override
        public @Nullable String convert(@Nullable Object object, @NotNull ConvertErrorRecorder errorRecorder) {
            return object == null ? null : object.toString().replace(".skin", "");
        }

        @Override
        public @Nullable Object toExportValue(String value) {
            return value;
        }
    }, listOf("xknat", "pistonmaster"));
    @Comment({
            "<!! Warning !!>",
            "Enable this will require players to run \"/skin update\" to update their skin."
    })
    public static final Property<Boolean> DISALLOW_AUTO_UPDATE_SKIN = newProperty("storage.disallowAutoUpdateSkin", false);
    @Comment({
            "Time that skins are stored in the database before we request again (in minutes).",
            "[?] A value of 0 will disable auto updating of skins and players will need to manual run /skin update.",
            "[!] Lowering this value will increase the amount of requests which could be a problem on large servers."
    })
    public static final Property<Integer> SKIN_EXPIRES_AFTER = newProperty("storage.skinExpiresAfter", 15);
    @Comment("How long we should cache the UUIDs of players (in minutes).")
    public static final Property<Integer> UUID_EXPIRES_AFTER = newProperty("storage.uuidExpiresAfter", 60);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("storage",
                "\n",
                "\n###########",
                "\n# Storage #",
                "\n###########",
                "\n",
                "Here you can design the plugin the way you want it.",
                "\n",
                "Enable or disable default skins",
                "applyForPremium: false will only put a skin on skinless/steve players.",
                "If there is more than one, the plugin will choose a random one.",
                "[?] Supports custom & url.png skins, read SkinFile Generator below. [?]"
        );
    }
}
