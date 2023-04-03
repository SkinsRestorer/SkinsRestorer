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

import lombok.Getter;

import java.util.Optional;

public enum PermissionRegistry {
    SKIN("skinsrestorer.command", true),
    SR("skinsrestorer.admincommand"),
    SKINS("skinsrestorer.command.gui", true),
    SKIN_SET("skinsrestorer.command.set", true),
    SKIN_SET_OTHER("skinsrestorer.command.set.other"),
    SKIN_SET_URL("skinsrestorer.command.set.url", true),
    SKIN_CLEAR("skinsrestorer.command.clear", true),
    SKIN_CLEAR_OTHER("skinsrestorer.command.clear.other"),
    SKIN_SEARCH("skinsrestorer.command.search", true),
    SKIN_UPDATE("skinsrestorer.command.update", true),
    SKIN_UPDATE_OTHER("skinsrestorer.command.update.other"),
    SR_RELOAD("skinsrestorer.admincommand.reload"),
    SR_STATUS("skinsrestorer.admincommand.status"),
    SR_DROP("skinsrestorer.admincommand.drop"),
    SR_PROPS("skinsrestorer.admincommand.props"),
    SR_APPLY_SKIN("skinsrestorer.admincommand.applyskin"),
    SR_CREATE_CUSTOM("skinsrestorer.admincommand.createcustom"),
    SR_DUMP("skinsrestorer.admincommand.dump"),

    BYPASS_COOLDOWN("skinsrestorer.bypasscooldown"),
    BYPASS_DISABLED("skinsrestorer.bypassdisabled"),
    OWN_SKIN("skinsrestorer.ownskin");

    @Getter
    private final Permission permission;

    PermissionRegistry(String permission) {
        this(permission, false);
    }

    PermissionRegistry(String permission, boolean isDefault) {
        this.permission = Permission.of(permission, isDefault);
    }

    public static Permission forSkin(String skinName) {
        return Permission.of("skinsrestorer.skin." + skinName, false);
    }

    public static Optional<Permission> getPermission(String permission) {
        for (PermissionRegistry value : values()) {
            if (value.permission.getPermissionString().equals(permission))
                return Optional.of(value.permission);
        }

        return Optional.empty();
    }
}
