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
package net.skinsrestorer.shared.storage;

import co.aikar.locales.MessageKey;
import lombok.Getter;

public enum Message {
    PREFIX,
    HELP_HELP_COMMAND,
    HELP_SKIN_SET,
    HELP_SKIN_SET_OTHER,
    HELP_SKIN_SET_OTHER_URL,
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
    HELP_SR_CREATECUSTOM,
    SYNTAX_DEFAULTCOMMAND,
    SYNTAX_SKINSET,
    SYNTAX_SKINSET_OTHER,
    SYNTAX_SKINURL,
    SYNTAX_SKINSEARCH,
    SYNTAX_SKINUPDATE_OTHER,
    SYNTAX_SKINCLEAR_OTHER,
    COMPLETIONS_SKIN,
    COMPLETIONS_SKINNAME,
    COMPLETIONS_SKINURL,
    COMMAND_SERVER_NOT_ALLOWED_MESSAGE,
    PLAYER_HAS_NO_PERMISSION_SKIN,
    PLAYER_HAS_NO_PERMISSION_URL,
    ERROR_SKIN_DISABLED,
    ERROR_SKINURL_DISALLOWED,
    NOT_PREMIUM,
    ONLY_ALLOWED_ON_CONSOLE,
    INVALID_PLAYER,
    SKIN_COOLDOWN,
    SUCCESS_SKIN_CHANGE,
    SUCCESS_SKIN_CLEAR,
    SUCCESS_SKIN_CLEAR_OTHER,
    MS_UPDATING_SKIN,
    SUCCESS_ADMIN_CREATECUSTOM,
    SUCCESS_UPDATING_SKIN,
    SUCCESS_UPDATING_SKIN_OTHER,
    ERROR_UPDATING_SKIN,
    ERROR_UPDATING_URL,
    ERROR_UPDATING_CUSTOMSKIN,
    ERROR_INVALID_URLSKIN,
    ERROR_MS_FULL,
    ERROR_MS_GENERIC,
    GENERIC_ERROR,
    WAIT_A_MINUTE,
    ERROR_NO_SKIN,
    SKINSMENU_OPEN,
    SKINSMENU_TITLE_NEW,
    SKINSMENU_NEXT_PAGE,
    SKINSMENU_PREVIOUS_PAGE,
    SKINSMENU_CLEAR_SKIN,
    SKINSMENU_SELECT_SKIN,
    SKIN_SEARCH_MESSAGE,
    SUCCESS_SKIN_CHANGE_OTHER,
    SUCCESS_ADMIN_DROP,
    SUCCES_ADMIN_APPLYSKIN,
    ERROR_ADMIN_APPLYSKIN,
    STATUS_OK,
    ALT_API_FAILED,
    ERROR_MS_API_FAILED,
    NO_SKIN_DATA,
    SUCCESS_ADMIN_RELOAD,
    OUTDATED,
    SR_LINE,
    CUSTOM_HELP_IF_ENABLED;

    @Getter
    private final MessageKey key = MessageKey.of("skinsrestorer." + this.name().toLowerCase());
}
