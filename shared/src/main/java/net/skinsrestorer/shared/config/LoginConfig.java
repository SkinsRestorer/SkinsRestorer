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

public class LoginConfig implements SettingsHolder {
    @Comment({
            "Stops the process of setting a skin if the LoginEvent was canceled by an AntiBot plugin.",
            "[?] Unsure? leave this true for better performance."
    })
    public static final Property<Boolean> NO_SKIN_IF_LOGIN_CANCELED = newProperty("login.noSkinIfLoginCanceled", true);
    @Comment("This will make SkinsRestorer always apply the skin even if the player joins as premium on an online mode server.")
    public static final Property<Boolean> ALWAYS_APPLY_PREMIUM = newProperty("login.alwaysApplyPremium", false);
}
