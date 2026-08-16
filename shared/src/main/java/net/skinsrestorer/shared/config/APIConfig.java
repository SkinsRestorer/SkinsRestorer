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
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class APIConfig implements SettingsHolder {
    @Comment({
            "Add a MineSkin API key to reduce request times.",
            "Request a key at https://mineskin.org/apikey",
            "MineSkin offers 10% off your first three months (all plans except Lite) with the coupon SKINSRESTORER10.",
            "All plans other than Lite include cape generation, higher limits, and other exclusive features.",
            "The API key is optional."
    })
    public static final Property<String> MINESKIN_API_KEY = newProperty("api.mineskinAPIKey", "key");
    @Comment({
            "SkinsRestorer uses MineSkin to generate usable skins from links.",
            "By default, MineSkin shows generated skins in its public gallery.",
            "Enable this option to hide generated skins from the gallery."
    })
    public static final Property<Boolean> MINESKIN_SECRET_SKINS = newProperty("api.mineskinSecretSkins", false);
    @Comment({
            "SkinsRestorer provides a list of recommended skins.",
            "Players can use these skins through /skin random and the /skins GUI.",
            "Disable this option to stop loading or downloading recommended skins.",
            "The /skin random command does not work when this option is disabled."
    })
    public static final Property<Boolean> FETCH_RECOMMENDED_SKINS = newProperty("api.fetchRecommendedSkins", true);
    @Comment({
            "The time window, in seconds, for each batch of Mojang API requests.",
            "A batch ends when this time expires or when it contains 10 requests.",
            "The default value is 1 second."
    })
    public static final Property<Integer> MOJANG_BATCH_WINDOW_SECONDS = newProperty("api.mojangBatchWindowSeconds", 1);
    @Comment({
            "Enables Ely.by as a skin provider.",
            "When enabled, SkinsRestorer gets player skins from Ely.by instead of Mojang.",
            "Enable this option only if your server uses Ely.by accounts."
    })
    public static final Property<Boolean> ELYBY_ENABLED = newProperty("api.elyByEnabled", false);
}
