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

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.utils.Tristate;

import java.util.Collection;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class Permission {
    private final String permissionString;

    public boolean checkPermission(SettingsManager settings, Function<String, Tristate> predicate) {
        Collection<PermissionGroup> permissionGroups = PermissionGroup.getGrantedBy(this);
        if (permissionGroups.isEmpty()) {
            return internalCheckPermission(predicate).asBoolean();
        }

        Tristate tristate = internalCheckPermission(predicate);

        // The permission was set explicitly, so we don't need to check the groups.
        if (tristate != Tristate.UNDEFINED) {
            return tristate.asBoolean();
        }

        for (PermissionGroup permissionGroup : permissionGroups) {
            if (permissionGroup == PermissionGroup.DEFAULT_GROUP
                    && settings.getProperty(CommandConfig.FORCE_DEFAULT_PERMISSIONS)) {
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
}
