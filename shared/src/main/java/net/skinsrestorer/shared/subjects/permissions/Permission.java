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
package net.skinsrestorer.shared.subjects.permissions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.utils.Tristate;

import java.util.function.Function;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(staticName = "of")
public class Permission {
    private final String permissionString;

    public boolean checkPermission(Function<String, Tristate> predicate) {
        Tristate tristate = internalCheckPermission(predicate);

        // If it was set explicitly, we don't need to check the groups or inheritance.
        if (tristate != Tristate.UNDEFINED) {
            return tristate.asBoolean();
        }

        for (PermissionGroup permissionGroup : PermissionGroup.getGrantedBy(this)) {
            if (permissionGroup == PermissionGroup.DEFAULT_GROUP) {
                return true;
            }

            Tristate groupTristate = permissionGroup.getBasePermission().internalCheckPermission(predicate);
            if (groupTristate != Tristate.UNDEFINED) {
                return groupTristate.asBoolean();
            }

            Tristate wildcardTristate = permissionGroup.getWildcard().internalCheckPermission(predicate);
            if (wildcardTristate != Tristate.UNDEFINED) {
                return wildcardTristate.asBoolean();
            }
        }

        return false;
    }

    public Tristate internalCheckPermission(Function<String, Tristate> predicate) {
        return predicate.apply(permissionString);
    }

    public boolean isInDefaultGroup() {
        return PermissionGroup.getGrantedBy(this).contains(PermissionGroup.DEFAULT_GROUP);
    }
}
