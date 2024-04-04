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
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class APIConfig implements SettingsHolder {
    @Comment({
            "Here you can fill in your APIKey for lower MineSkin request times.",
            "Key can be requested from https://mineskin.org/apikey",
            "[?] A key is not required, but recommended."
    })
    public static final Property<String> MINESKIN_API_KEY = newProperty("api.mineskinAPIKey", "key");
    @Comment({
            "This option disables the use of api.ashcon.app",
            "It is recommended to keep this enabled, as it is used for fetching and caching skins.",
            "You may hit Mojang ratelimits if you disable this, as it will fall back to Mojang and then MineTools",
            "Both of which are not sufficient for servers with many players."
    })
    public static final Property<Boolean> DISABLE_ASHCON = newProperty("api.disableAshcon", false);
}
