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
package net.skinsrestorer.shared.commands.library;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;

import java.util.function.Predicate;

@ToString
@Setter
@Getter
@AllArgsConstructor
public class PermissionPredicate<T> implements Predicate<T> {
    private final CommandPlatform<T> platform;
    private PermissionRegistry permission;

    @Override
    public boolean test(T t) {
        return platform.detectAndConvertSender(t).hasPermission(permission);
    }
}
