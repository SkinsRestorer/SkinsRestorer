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

import lombok.Getter;
import net.skinsrestorer.shared.subjects.messages.Message;

@Getter
public enum PermissionRegistry {
    SKIN("skinsrestorer.command", Message.PERMISSION_COMMAND),
    SR("skinsrestorer.admincommand", Message.PERMISSION_ADMINCOMMAND),
    SKULL("skinsrestorer.command.skull", Message.PERMISSION_COMMAND),
    SKINS("skinsrestorer.command.gui", Message.PERMISSION_COMMAND_GUI),
    SKIN_SET("skinsrestorer.command.set", Message.PERMISSION_COMMAND_SET),
    SKIN_SET_OTHER("skinsrestorer.command.set.other", Message.PERMISSION_COMMAND_SET_OTHER),
    SKIN_SET_URL("skinsrestorer.command.set.url", Message.PERMISSION_COMMAND_SET_URL),
    SKIN_CLEAR("skinsrestorer.command.clear", Message.PERMISSION_COMMAND_CLEAR),
    SKIN_CLEAR_OTHER("skinsrestorer.command.clear.other", Message.PERMISSION_COMMAND_CLEAR_OTHER),
    SKIN_RANDOM("skinsrestorer.command.random", Message.PERMISSION_COMMAND_RANDOM),
    SKIN_RANDOM_OTHER("skinsrestorer.command.random.other", Message.PERMISSION_COMMAND_RANDOM_OTHER),
    SKIN_SEARCH("skinsrestorer.command.search", Message.PERMISSION_COMMAND_SEARCH),
    SKIN_EDIT("skinsrestorer.command.edit", Message.PERMISSION_COMMAND_EDIT),
    SKIN_UPDATE("skinsrestorer.command.update", Message.PERMISSION_COMMAND_UPDATE),
    SKIN_UPDATE_OTHER("skinsrestorer.command.update.other", Message.PERMISSION_COMMAND_UPDATE_OTHER),
    SKIN_UNDO("skinsrestorer.command.undo", Message.PERMISSION_COMMAND_UNDO),
    SKIN_UNDO_OTHER("skinsrestorer.command.undo.other", Message.PERMISSION_COMMAND_UNDO_OTHER),
    SKIN_FAVOURITE("skinsrestorer.command.favourite", Message.PERMISSION_COMMAND_FAVOURITE),
    SKIN_FAVOURITE_OTHER("skinsrestorer.command.favourite.other", Message.PERMISSION_COMMAND_FAVOURITE_OTHER),
    SKULL_GET("skinsrestorer.admincommand.skull.get", Message.PERMISSION_ADMINCOMMAND_SKULL_GET),
    SKULL_GET_OTHER("skinsrestorer.admincommand.skull.get.other", Message.PERMISSION_ADMINCOMMAND_SKULL_GET_OTHER),
    SKULL_GET_URL("skinsrestorer.admincommand.skull.get.url", Message.PERMISSION_ADMINCOMMAND_SKULL_GET_URL),
    SKULL_RANDOM("skinsrestorer.admincommand.skull.random", Message.PERMISSION_ADMINCOMMAND_SKULL_RANDOM),
    SKULL_RANDOM_OTHER("skinsrestorer.admincommand.skull.random.other", Message.PERMISSION_ADMINCOMMAND_SKULL_RANDOM_OTHER),
    SR_RELOAD("skinsrestorer.admincommand.reload", Message.PERMISSION_ADMINCOMMAND_RELOAD),
    SR_STATUS("skinsrestorer.admincommand.status", Message.PERMISSION_ADMINCOMMAND_STATUS),
    SR_DROP("skinsrestorer.admincommand.drop", Message.PERMISSION_ADMINCOMMAND_DROP),
    SR_INFO("skinsrestorer.admincommand.info", Message.PERMISSION_ADMINCOMMAND_INFO),
    SR_APPLY_SKIN("skinsrestorer.admincommand.applyskin", Message.PERMISSION_ADMINCOMMAND_APPLYSKIN),
    SR_CREATE_CUSTOM("skinsrestorer.admincommand.createcustom", Message.PERMISSION_ADMINCOMMAND_CREATECUSTOM),
    SR_PURGE_OLD_DATA("skinsrestorer.admincommand.purgeolddata", Message.PERMISSION_ADMINCOMMAND_PURGEOLDDATA),
    SR_DUMP("skinsrestorer.admincommand.dump", Message.PERMISSION_ADMINCOMMAND_DUMP),

    BYPASS_COOLDOWN("skinsrestorer.bypasscooldown", Message.PERMISSION_BYPASSCOOLDOWN),
    BYPASS_DISABLED("skinsrestorer.bypassdisabled", Message.PERMISSION_BYPASSDISABLED),
    OWN_SKIN("skinsrestorer.ownskin", Message.PERMISSION_OWNSKIN);

    private final Permission permission;
    private final Message description;

    PermissionRegistry(String permission, Message description) {
        this.permission = Permission.of(permission);
        this.description = description;
    }

    public static Permission forSkin(String skinName) {
        return Permission.of("skinsrestorer.skin.%s".formatted(skinName));
    }
}
