/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.subjects.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public enum Message {
    PREFIX_FORMAT,
    HELP_SKIN,
    HELP_SKINS,
    HELP_SR,
    HELP_SKIN_SET,
    HELP_SKIN_SET_OTHER,
    HELP_SKIN_SET_URL,
    HELP_SKIN_CLEAR,
    HELP_SKIN_CLEAR_OTHER,
    HELP_SKIN_SEARCH,
    HELP_SKIN_UPDATE,
    HELP_SKIN_UPDATE_OTHER,
    HELP_SR_RELOAD,
    HELP_SR_STATUS,
    HELP_SR_DROP,
    HELP_SR_PROPS,
    HELP_SR_APPLY_SKIN,
    HELP_SR_CREATE_CUSTOM,
    HELP_SR_SET_SKIN_ALL,
    HELP_SR_APPLY_SKIN_ALL,
    HELP_SR_PURGE_OLD_DATA,
    HELP_SR_DUMP,
    SUCCESS_SKIN_CHANGE(true),
    SUCCESS_SKIN_CHANGE_OTHER(true),
    SUCCESS_SKIN_CLEAR(true),
    SUCCESS_SKIN_CLEAR_OTHER(true),
    SUCCESS_UPDATING_SKIN(true),
    SUCCESS_UPDATING_SKIN_OTHER(true),
    SUCCESS_ADMIN_APPLYSKIN(true),
    SUCCESS_ADMIN_CREATECUSTOM(true),
    SUCCESS_ADMIN_DROP(true),
    SUCCESS_ADMIN_RELOAD(true),
    ERROR_PLAYER_NOT_FOUND(true),
    ERROR_SKIN_DISABLED(true),
    ERROR_SKINURL_DISALLOWED(true),
    ERROR_UPDATING_SKIN(true),
    ERROR_UPDATING_URL(true),
    ERROR_UPDATING_CUSTOMSKIN(true),
    ERROR_INVALID_URLSKIN(true),
    ERROR_ADMIN_APPLYSKIN(true),
    ERROR_MS_FULL(true),
    ERROR_MS_API_FAILED(true),
    ERROR_MS_API_KEY_INVALID(true),
    ERROR_MS_UNKNOWN(true),
    NOT_CONNECTED_TO_SERVER(true),
    ADMINCOMMAND_STATUS_CHECKING(true),
    ADMINCOMMAND_STATUS_WORKING_COUNT(true),
    ADMINCOMMAND_STATUS_WORKING(true),
    ADMINCOMMAND_STATUS_BROKEN(true),
    ADMINCOMMAND_STATUS_SUMMARY_VERSION(true),
    ADMINCOMMAND_STATUS_SUMMARY_SERVER(true),
    ADMINCOMMAND_STATUS_SUMMARY_PROXYMODE(true),
    ADMINCOMMAND_STATUS_SUMMARY_COMMIT(true),
    ADMINCOMMAND_STATUS_SUMMARY_FINISHED(true),
    ADMINCOMMAND_DROP_PLAYER_NOT_FOUND(true),
    ADMINCOMMAND_DROP_UUID_ERROR(true),
    ADMINCOMMAND_DROP_SKIN_NOT_FOUND(true),
    ADMINCOMMAND_PROPS_REQUEST_TIME(true),
    ADMINCOMMAND_PROPS_PROFILE_ID(true),
    ADMINCOMMAND_PROPS_NAME(true),
    ADMINCOMMAND_PROPS_SKIN_TEXTURE(true),
    ADMINCOMMAND_PROPS_MORE_INFO_IN_CONSOLE(true),
    ADMINCOMMAND_SETSKINALL_NOT_FOUND(true),
    ADMINCOMMAND_SETSKINALL_SUCCESS(true),
    ADMINCOMMAND_APPLYSKINALL_PLAYER_ERROR(true),
    ADMINCOMMAND_APPLYSKINALL_SUCCESS(true),
    ADMINCOMMAND_PURGEOLDDATA_SUCCESS(true),
    ADMINCOMMAND_PURGEOLDDATA_ERROR(true),
    ADMINCOMMAND_DUMP_UPLOADING(true),
    ADMINCOMMAND_DUMP_SUCCESS(true),
    ADMINCOMMAND_DUMP_ERROR(true),
    COMMAND_SERVER_NOT_ALLOWED_MESSAGE(true),
    PLAYER_HAS_NO_PERMISSION_SKIN(true),
    PLAYER_HAS_NO_PERMISSION_URL(true),
    NOT_PREMIUM(true),
    ONLY_ALLOWED_ON_CONSOLE(true),
    ONLY_ALLOWED_ON_PLAYER(true),
    INVALID_PLAYER(true),
    SKIN_COOLDOWN(true),
    MS_UPDATING_SKIN(true),
    GENERIC_ERROR(true),
    WAIT_A_MINUTE(true),
    PERMISSION_PLAYER_WILDCARD,
    PERMISSION_COMMAND,
    PERMISSION_COMMAND_SET,
    PERMISSION_COMMAND_SET_URL,
    PERMISSION_COMMAND_CLEAR,
    PERMISSION_COMMAND_UPDATE,
    PERMISSION_COMMAND_SEARCH,
    PERMISSION_COMMAND_GUI,
    PERMISSION_ADMIN_WILDCARD,
    PERMISSION_ADMINCOMMAND,
    PERMISSION_COMMAND_SET_OTHER,
    PERMISSION_COMMAND_CLEAR_OTHER,
    PERMISSION_COMMAND_UPDATE_OTHER,
    PERMISSION_ADMINCOMMAND_RELOAD,
    PERMISSION_ADMINCOMMAND_STATUS,
    PERMISSION_ADMINCOMMAND_DROP,
    PERMISSION_ADMINCOMMAND_PROPS,
    PERMISSION_ADMINCOMMAND_APPLYSKIN,
    PERMISSION_ADMINCOMMAND_CREATECUSTOM,
    PERMISSION_ADMINCOMMAND_APPLYSKINALL,
    PERMISSION_ADMINCOMMAND_PURGEOLDDATA,
    PERMISSION_ADMINCOMMAND_DUMP,
    PERMISSION_BYPASSCOOLDOWN,
    PERMISSION_BYPASSDISABLED,
    PERMISSION_OWNSKIN,
    SKINSMENU_OPEN(true),
    SKINSMENU_TITLE_NEW,
    SKINSMENU_NEXT_PAGE,
    SKINSMENU_PREVIOUS_PAGE,
    SKINSMENU_CLEAR_SKIN,
    SKINSMENU_SELECT_SKIN,
    SKIN_SEARCH_MESSAGE(true),
    NO_SKIN_DATA(true),
    OUTDATED(true);

    private final String key = "skinsrestorer." + this.name().toLowerCase(Locale.ENGLISH);
    @Getter
    private final boolean prefixed;

    Message() {
        this(false);
    }

    public static Message fromKey(String key) {
        for (Message message : values()) {
            if (message.key.equals(key)) {
                return message;
            }
        }

        throw new IllegalArgumentException(String.format("No message enum found for key %s", key));
    }
}
