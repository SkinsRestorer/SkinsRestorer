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
import net.skinsrestorer.shared.storage.Message;

public enum PermissionRegistry {
    SKIN("skinsrestorer.command", Message.PERMISSION_COMMAND),
    SR("skinsrestorer.admincommand", Message.PERMISSION_ADMINCOMMAND),
    SKINS("skinsrestorer.command.gui", Message.PERMISSION_COMMAND_GUI),
    SKIN_SET("skinsrestorer.command.set", Message.PERMISSION_COMMAND_SET),
    SKIN_SET_OTHER("skinsrestorer.command.set.other", Message.PERMISSION_COMMAND_SET_OTHER),
    SKIN_SET_URL("skinsrestorer.command.set.url", Message.PERMISSION_COMMAND_SET_URL),
    SKIN_CLEAR("skinsrestorer.command.clear", Message.PERMISSION_COMMAND_CLEAR),
    SKIN_CLEAR_OTHER("skinsrestorer.command.clear.other", Message.PERMISSION_COMMAND_CLEAR_OTHER),
    SKIN_SEARCH("skinsrestorer.command.search", Message.PERMISSION_COMMAND_SEARCH),
    SKIN_UPDATE("skinsrestorer.command.update", Message.PERMISSION_COMMAND_UPDATE),
    SKIN_UPDATE_OTHER("skinsrestorer.command.update.other", Message.PERMISSION_COMMAND_UPDATE_OTHER),
    SR_RELOAD("skinsrestorer.admincommand.reload", Message.PERMISSION_ADMINCOMMAND_RELOAD),
    SR_STATUS("skinsrestorer.admincommand.status", Message.PERMISSION_ADMINCOMMAND_STATUS),
    SR_DROP("skinsrestorer.admincommand.drop", Message.PERMISSION_ADMINCOMMAND_DROP),
    SR_PROPS("skinsrestorer.admincommand.props", Message.PERMISSION_ADMINCOMMAND_PROPS),
    SR_APPLY_SKIN("skinsrestorer.admincommand.applyskin", Message.PERMISSION_ADMINCOMMAND_APPLYSKIN),
    SR_CREATE_CUSTOM("skinsrestorer.admincommand.createcustom", Message.PERMISSION_ADMINCOMMAND_CREATECUSTOM),
    SR_APPLY_SKIN_ALL("skinsrestorer.admincommand.applyskinall", Message.PERMISSION_ADMINCOMMAND_APPLYSKINALL),
    SR_PURGE_OLD_DATA("skinsrestorer.admincommand.purgeolddata", Message.PERMISSION_ADMINCOMMAND_PURGEOLDDATA),
    SR_DUMP("skinsrestorer.admincommand.dump", Message.PERMISSION_ADMINCOMMAND_DUMP),

    BYPASS_COOLDOWN("skinsrestorer.bypasscooldown", Message.PERMISSION_BYPASSCOOLDOWN),
    BYPASS_DISABLED("skinsrestorer.bypassdisabled", Message.PERMISSION_BYPASSDISABLED),
    OWN_SKIN("skinsrestorer.ownskin", Message.PERMISSION_OWNSKIN);

    @Getter
    private final Permission permission;
    @Getter
    private final Message description;

    PermissionRegistry(String permission, Message description) {
        this.permission = Permission.of(permission);
        this.description = description;
    }

    public static Permission forSkin(String skinName) {
        return Permission.of("skinsrestorer.skin." + skinName);
    }
}
