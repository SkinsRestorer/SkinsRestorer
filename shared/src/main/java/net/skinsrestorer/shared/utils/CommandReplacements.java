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

import net.skinsrestorer.shared.storage.CallableSetting;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;

import java.util.Map;

public class CommandReplacements {
    public static final Map<String, CallableSetting<String>> permissions = FluentMap.<String, CallableSetting<String>>builder()
            .put("skin", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command")
            .put("sr", (s) -> "skinsrestorer.admincommand")

            .put("skins", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command.gui")

            .put("skinSet", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command.set")
            .put("skinSetOther", (s) -> "skinsrestorer.command.set.other")

            .put("skinSetUrl", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command.set.url")

            .put("skinClear", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command.clear")
            .put("skinClearOther", (s) -> "skinsrestorer.command.clear.other")

            .put("skinSearch", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command.search")

            .put("skinUpdate", (s) -> s.getProperty(Config.SKIN_WITHOUT_PERM) ? "" : "skinsrestorer.command.update")
            .put("skinUpdateOther", (s) -> "skinsrestorer.command.update.other")

            .put("srReload", (s) -> "skinsrestorer.admincommand.reload")
            .put("srStatus", (s) -> "skinsrestorer.admincommand.status")
            .put("srDrop", (s) -> "skinsrestorer.admincommand.drop")
            .put("srProps", (s) -> "skinsrestorer.admincommand.props")
            .put("srApplySkin", (s) -> "skinsrestorer.admincommand.applyskin")
            .put("srCreateCustom", (s) -> "skinsrestorer.admincommand.createcustom")
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
