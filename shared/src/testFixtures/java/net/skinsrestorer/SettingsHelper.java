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
package net.skinsrestorer;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.properties.Property;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class SettingsHelper {
    @SuppressWarnings("unchecked")
    public static void returnDefaultsForAllProperties(SettingsManager settings) {
        given(settings.getProperty(any(Property.class)))
                .willAnswer(invocation -> ((Property<?>) invocation.getArgument(0)).getDefaultValue());
    }
}
