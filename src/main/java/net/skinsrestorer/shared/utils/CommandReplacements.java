package net.skinsrestorer.shared.utils;

import com.google.common.collect.ImmutableMap;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;

import java.util.Map;

/**
 * Created by McLive on 25.01.2019.
 */
public class CommandReplacements {
    private CommandReplacements() {
    }

    public static final Map<String, String> permissions = ImmutableMap.<String, String>builder()
            .put("skin", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command")
            .put("sr", "skinsrestorer.admincommand")

            .put("skins", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.gui")

            .put("skinSet", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.set")
            .put("skinSetOther", "skinsrestorer.command.set.other")

            .put("skinSetUrl", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.set.url")

            .put("skinClear", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.clear")
            .put("skinClearOther", "skinsrestorer.command.clear.other")

            .put("skinUpdate", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.update")
            .put("skinUpdateOther", "skinsrestorer.command.update.other")

            .put("srReload", "skinsrestorer.admincommand.reload")
            .put("srStatus", "skinsrestorer.admincommand.status")
            .put("srDrop", "skinsrestorer.admincommand.drop")
            .put("srProps", "skinsrestorer.admincommand.props")
            .build();

    public static final Map<String, String> descriptions = ImmutableMap.<String, String>builder()
            .put("%helpSkinClear", Locale.HELP_SKIN_CLEAR)
            .put("%helpSkinClearOther", Locale.HELP_SKIN_CLEAR_OTHER)
            .put("%helpSkinUpdate", Locale.HELP_SKIN_UPDATE)
            .put("%helpSkinUpdateOther", Locale.HELP_SKIN_UPDATE_OTHER)
            .put("%helpSkinSet", Locale.HELP_SKIN_SET)
            .put("%helpSkinSetOther", Locale.HELP_SKIN_SET_OTHER)
            .put("%helpSkinSetUrl", Locale.HELP_SKIN_SET_OTHER_URL)
            .put("%helpSrReload", Locale.HELP_SR_RELOAD)
            .put("%helpSrStatus", Locale.HELP_SR_STATUS)
            .put("%helpSrDrop", Locale.HELP_SR_DROP)
            .put("%helpSrProps", Locale.HELP_SR_PROPS)
            .build();

    public static final Map<String, String> syntax = ImmutableMap.<String, String>builder()
            .put("%SyntaxDefaultCommand", Locale.SYNTAX_DEFAULTCOMMAND)
            .put("%SyntaxSkinSet", Locale.SYNTAX_SKINSET)
            .put("%SyntaxSkinSetOther", Locale.SYNTAX_SKINSET_OTHER)
            .put("%SyntaxSkinUrl", Locale.SYNTAX_SKINURL)
            .put("%SyntaxSkinUpdateOther", Locale.SYNTAX_SKINUPDATE_OTHER)
            .put("%SyntaxSkinClearOther", Locale.SYNTAX_SKINCLEAR_OTHER)
            .build();
}
