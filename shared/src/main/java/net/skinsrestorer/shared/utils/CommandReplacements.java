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
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.storage.CallableString;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;

import java.util.Map;

public class CommandReplacements {
    public static final Map<String, CallableString<String>> permissions = FluentMap.<String, CallableString<String>>builder()
            .put("skin", () -> Config.SKIN_WITHOUT_PERM ? "" : "skinsrestorer.command")
            .put("sr", () -> "skinsrestorer.admincommand")

            .put("skins", () -> Config.SKIN_WITHOUT_PERM ? "" : "skinsrestorer.command.gui")

            .put("skinSet", () -> Config.SKIN_WITHOUT_PERM ? "" : "skinsrestorer.command.set")
            .put("skinSetOther", () -> "skinsrestorer.command.set.other")

            .put("skinSetUrl", () -> Config.SKIN_WITHOUT_PERM ? "" : "skinsrestorer.command.set.url")

            .put("skinClear", () -> Config.SKIN_WITHOUT_PERM ? "" : "skinsrestorer.command.clear")
            .put("skinClearOther", () -> "skinsrestorer.command.clear.other")

            .put("skinUpdate", () -> Config.SKIN_WITHOUT_PERM ? "" : "skinsrestorer.command.update")
            .put("skinUpdateOther", () -> "skinsrestorer.command.update.other")

            .put("srReload", () -> "skinsrestorer.admincommand.reload")
            .put("srStatus", () -> "skinsrestorer.admincommand.status")
            .put("srDrop", () -> "skinsrestorer.admincommand.drop")
            .put("srProps", () -> "skinsrestorer.admincommand.props")
            .put("srApplySkin", () -> "skinsrestorer.admincommand.applyskin")
            .put("srCreateCustom", () -> "skinsrestorer.admincommand.createcustom")
            .build();
    public static final Map<String, CallableString<String>> descriptions = FluentMap.<String, CallableString<String>>builder()
            .put("%helpSkinClear", () -> Locale.HELP_SKIN_CLEAR)
            .put("%helpSkinClearOther", () -> Locale.HELP_SKIN_CLEAR_OTHER)
            .put("%helpSkinUpdate", () -> Locale.HELP_SKIN_UPDATE)
            .put("%helpSkinUpdateOther", () -> Locale.HELP_SKIN_UPDATE_OTHER)
            .put("%helpSkinSet", () -> Locale.HELP_SKIN_SET)
            .put("%helpSkinSetOther", () -> Locale.HELP_SKIN_SET_OTHER)
            .put("%helpSkinSetUrl", () -> Locale.HELP_SKIN_SET_OTHER_URL)

            .put("%helpSrReload", () -> Locale.HELP_SR_RELOAD)
            .put("%helpSrStatus", () -> Locale.HELP_SR_STATUS)
            .put("%helpSrDrop", () -> Locale.HELP_SR_DROP)
            .put("%helpSrProps", () -> Locale.HELP_SR_PROPS)
            .put("%helpSrApplySkin", () -> Locale.HELP_SR_APPLY_SKIN)
            .put("%helpSrCreateCustom", () -> Locale.HELP_SR_CREATECUSTOM)
            .build();
    public static final Map<String, CallableString<String>> syntax = FluentMap.<String, CallableString<String>>builder()
            .put("%SyntaxDefaultCommand", () -> Locale.SYNTAX_DEFAULTCOMMAND)
            .put("%SyntaxSkinSet", () -> Locale.SYNTAX_SKINSET)
            .put("%SyntaxSkinSetOther", () -> Locale.SYNTAX_SKINSET_OTHER)
            .put("%SyntaxSkinUrl", () -> Locale.SYNTAX_SKINURL)
            .put("%SyntaxSkinUpdateOther", () -> Locale.SYNTAX_SKINUPDATE_OTHER)
            .put("%SyntaxSkinClearOther", () -> Locale.SYNTAX_SKINCLEAR_OTHER)
            .build();

    public static final Map<String, CallableString<String>> completions = FluentMap.<String, CallableString<String>>builder()
            .put("skin", () -> Locale.COMPLETIONS_SKIN)
            .put("skinName", () -> Locale.COMPLETIONS_SKINNAME)
            .put("skinUrl", () -> Locale.COMPLETIONS_SKINURL)
            .build();

    private CommandReplacements() {
    }
}
