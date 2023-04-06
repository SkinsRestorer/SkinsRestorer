/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.shared.subjects;

public enum PermissionGroup {
    PLAYER(
            Permission.of("skinsrestorer.player"),
            PermissionRegistry.SKIN,
            PermissionRegistry.SKIN_SET,
            PermissionRegistry.SKIN_SET_URL,
            PermissionRegistry.SKIN_CLEAR,
            PermissionRegistry.SKIN_UPDATE,
            PermissionRegistry.SKIN_SEARCH,
            PermissionRegistry.SKINS
    ),
    ADMIN(
            Permission.of("skinsrestorer.admin"),
            PermissionGroup.PLAYER,
            PermissionRegistry.SR,
            PermissionRegistry.SKIN_SET_OTHER,
            PermissionRegistry.SKIN_CLEAR_OTHER,
            PermissionRegistry.SKIN_UPDATE_OTHER,
            PermissionRegistry.SR_STATUS,
            PermissionRegistry.SR_DROP,
            PermissionRegistry.SR_PROPS,
            PermissionRegistry.SR_APPLY_SKIN,
            PermissionRegistry.SR_CREATE_CUSTOM,
            PermissionRegistry.BYPASS_COOLDOWN,
            PermissionRegistry.BYPASS_DISABLED
    );

    private final PermissionRegistry[] permissions;

    PermissionGroup(Permission basePermission, PermissionRegistry... permissions) {
        this.permissions = permissions;
    }

    PermissionGroup(Permission basePermission, PermissionGroup parent, PermissionRegistry... permissions) {
        this.permissions = permissions;
    }
}
