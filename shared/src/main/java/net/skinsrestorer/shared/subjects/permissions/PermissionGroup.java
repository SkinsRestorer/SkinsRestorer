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
package net.skinsrestorer.shared.subjects.permissions;

import lombok.Getter;
import net.skinsrestorer.shared.subjects.messages.Message;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public enum PermissionGroup {
    PLAYER(
            Permission.of("skinsrestorer.player"),
            Permission.of("skinsrestorer.command.*"),
            Message.PERMISSION_PLAYER_WILDCARD,
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
            Permission.of("skinsrestorer.admincommand.*"),
            Message.PERMISSION_ADMIN_WILDCARD,
            new PermissionGroup[]{PermissionGroup.PLAYER},
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

    private final Permission basePermission;
    private final Permission wildcard;
    private final Message description;
    private final PermissionGroup[] parents;
    private final PermissionRegistry[] permissions;

    PermissionGroup(Permission basePermission, Permission wildcard, Message description, PermissionRegistry... permissions) {
        this(basePermission, wildcard, description, new PermissionGroup[0], permissions);
    }

    PermissionGroup(Permission basePermission, Permission wildcard, Message description, PermissionGroup[] parents, PermissionRegistry... permissions) {
        this.basePermission = basePermission;
        this.wildcard = wildcard;
        this.description = description;
        this.parents = parents;
        this.permissions = permissions;
    }

    public static PermissionGroup getDefaultGroup() {
        return PLAYER;
    }

    public static Collection<PermissionGroup> getGrantedBy(Permission permission) {
        Set<PermissionGroup> groups = new HashSet<>();

        for (PermissionGroup group : values()) {
            if (group.hasPermission(permission)) {
                groups.add(group);
            }
        }

        return groups;
    }

    public boolean hasPermission(Permission permission) {
        for (PermissionRegistry registry : permissions) {
            if (registry.getPermission() == permission) {
                return true;
            }
        }

        for (PermissionGroup parent : parents) {
            if (parent.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }
}
