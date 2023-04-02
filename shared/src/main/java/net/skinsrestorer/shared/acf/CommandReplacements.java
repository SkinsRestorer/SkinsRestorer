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
package net.skinsrestorer.shared.acf;

import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.storage.CallableSetting;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.subjects.Permission;
import net.skinsrestorer.shared.subjects.Permissions;
import net.skinsrestorer.shared.utils.FluentMap;

import java.util.Map;

public class CommandReplacements {
    public static final Map<String, CallableSetting<Permission>> permissions = FluentMap.<String, CallableSetting<Permission>>builder()
            .put("skin", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKIN)
            .put("sr", (s) -> Permissions.SR)

            .put("skins", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKINS)

            .put("skinSet", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKIN_SET)
            .put("skinSetOther", (s) -> Permissions.SKIN_SET_OTHER)

            .put("skinSetUrl", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKIN_SET_URL)

            .put("skinClear", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKIN_CLEAR)
            .put("skinClearOther", (s) -> Permissions.SKIN_CLEAR_OTHER)

            .put("skinSearch", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKIN_SEARCH)

            .put("skinUpdate", (s) -> s.getProperty(CommandConfig.SKIN_WITHOUT_PERM) ? Permissions.EMPTY : Permissions.SKIN_UPDATE)
            .put("skinUpdateOther", (s) -> Permissions.SKIN_UPDATE_OTHER)

            .put("srReload", (s) -> Permissions.SR_RELOAD)
            .put("srStatus", (s) -> Permissions.SR_STATUS)
            .put("srDrop", (s) -> Permissions.SR_DROP)
            .put("srProps", (s) -> Permissions.SR_PROPS)
            .put("srApplySkin", (s) -> Permissions.SR_APPLY_SKIN)
            .put("srCreateCustom", (s) -> Permissions.SR_CREATE_CUSTOM)
            .put("srDumpsrDump", (s) -> Permissions.SR_DUMP)
            .build();

    public static final Map<String, Message> descriptions = FluentMap.<String, Message>builder()
            .put("%helpHelpCommand", Message.HELP_HELP_COMMAND)
            .put("%helpSkinClear", Message.HELP_SKIN_CLEAR)
            .put("%helpSkinClearOther", Message.HELP_SKIN_CLEAR_OTHER)
            .put("%helpSkinSearch", Message.HELP_SKIN_SEARCH)
            .put("%helpSkinUpdate", Message.HELP_SKIN_UPDATE)
            .put("%helpSkinUpdateOther", Message.HELP_SKIN_UPDATE_OTHER)
            .put("%helpSkinSet", Message.HELP_SKIN_SET)
            .put("%helpSkinSetOther", Message.HELP_SKIN_SET_OTHER)
            .put("%helpSkinSetUrl", Message.HELP_SKIN_SET_OTHER_URL)

            .put("%helpSrReload", Message.HELP_SR_RELOAD)
            .put("%helpSrStatus", Message.HELP_SR_STATUS)
            .put("%helpSrDrop", Message.HELP_SR_DROP)
            .put("%helpSrProps", Message.HELP_SR_PROPS)
            .put("%helpSrApplySkin", Message.HELP_SR_APPLY_SKIN)
            .put("%helpSrCreateCustom", Message.HELP_SR_CREATECUSTOM)
            .build();

    public static final Map<String, Message> syntax = FluentMap.<String, Message>builder()
            .put("%SyntaxDefaultCommand", Message.SYNTAX_DEFAULTCOMMAND)
            .put("%SyntaxSkinSet", Message.SYNTAX_SKINSET)
            .put("%SyntaxSkinSetOther", Message.SYNTAX_SKINSET_OTHER)
            .put("%SyntaxSkinUrl", Message.SYNTAX_SKINURL)
            .put("%SyntaxSkinSearch", Message.SYNTAX_SKINSEARCH)
            .put("%SyntaxSkinUpdateOther", Message.SYNTAX_SKINUPDATE_OTHER)
            .put("%SyntaxSkinClearOther", Message.SYNTAX_SKINCLEAR_OTHER)
            .build();

    public static final Map<String, Message> completions = FluentMap.<String, Message>builder()
            .put("skin", Message.COMPLETIONS_SKIN)
            .put("skinName", Message.COMPLETIONS_SKINNAME)
            .put("skinUrl", Message.COMPLETIONS_SKINURL)
            .build();

    private CommandReplacements() {
    }
}
