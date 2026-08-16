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
import ch.jalu.configme.properties.ListProperty;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newCappedProperty;

public class StorageConfig implements SettingsHolder {
    public static final Property<Boolean> DEFAULT_SKINS_ENABLED = newProperty("storage.defaultSkins.enabled", false);
    public static final Property<Boolean> DEFAULT_SKINS_PREMIUM = newProperty("storage.defaultSkins.applyForPremium", false);
    public static final Property<List<String>> DEFAULT_SKINS = new ListProperty<>("storage.defaultSkins.list", new PropertyType<>() {
        @Override
        public @Nullable String convert(@Nullable Object object, @NotNull ConvertErrorRecorder errorRecorder) {
            return object == null ? null : object.toString().replace(".skin", "");
        }

        @Override
        public @Nullable Object toExportValue(String value) {
            return value;
        }
    }, List.of("xknat", "pistonmaster", "<random>"));
    @Comment({
            "WARNING:",
            "When enabled, stored skins do not update automatically.",
            "Players must run /skin update to get a newer skin."
    })
    public static final Property<Boolean> DISALLOW_AUTO_UPDATE_SKIN = newProperty("storage.disallowAutoUpdateSkin", false);
    @Comment({
            "The skin cache duration, in minutes.",
            "A value of 0 requests the skin from Mojang every time.",
            "Lower values increase API requests and can cause rate limits on large servers."
    })
    public static final Property<Integer> SKIN_EXPIRES_AFTER = newCappedProperty("storage.skinExpiresAfter", 15, 0, Integer.MAX_VALUE);
    @Comment({
            "The player UUID cache duration, in minutes.",
            "A value of 0 requests the UUID from Mojang every time.",
            "Lower values increase API requests and can cause rate limits on large servers."
    })
    public static final Property<Integer> UUID_EXPIRES_AFTER = newCappedProperty("storage.uuidExpiresAfter", 60, 0, Integer.MAX_VALUE);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("storage",
                "\n",
                "\n###########",
                "\n# Storage #",
                "\n###########",
                "\n",
                "Controls stored skins and cache durations.",
                "\n",
                "Default skins:",
                "When applyForPremium is false, default skins apply only to players without a custom skin.",
                "If the list contains multiple skins, SkinsRestorer selects one at random.",
                "Use \"<random>\" to select a random recommended skin.",
                "Skins from image URLs are cached and do not update automatically."
        );
    }
}
