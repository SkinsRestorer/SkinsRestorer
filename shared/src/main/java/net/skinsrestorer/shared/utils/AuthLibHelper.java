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
package net.skinsrestorer.shared.utils;

public class AuthLibHelper {
    public static String getPropertyName(Object property) {
        try {
            return (String) property.getClass().getDeclaredField("name").get(property);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPropertyValue(Object property) {
        try {
            return (String) property.getClass().getDeclaredField("value").get(property);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPropertySignature(Object property) {
        try {
            return (String) property.getClass().getDeclaredField("signature").get(property);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
