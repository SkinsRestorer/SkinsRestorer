package skinsrestorer.shared.utils;

import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by McLive on 25.01.2019.
 */
public class CommandReplacements {
    public static Map<String, String> permissions = Stream.of(new String[][]{
            {"skin", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command"},
            {"sr", "skinsrestorer.admincommand"},

            {"skins", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.gui"},

            {"skinSet", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.set"},
            {"skinSetOther", "skinsrestorer.command.set.other"},

            {"skinSetUrl", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.set.url"},

            {"skinClear", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.clear"},
            {"skinClearOther", "skinsrestorer.command.clear.other"},

            {"skinUpdate", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.update"},
            {"skinUpdateOther", "skinsrestorer.command.update.other"},

            {"srReload", "skinsrestorer.admincommand.reload"},
            {"srStatus", "skinsrestorer.admincommand.status"},
            {"srDrop", "skinsrestorer.admincommand.drop"},
            {"srProps", "skinsrestorer.admincommand.props"},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    public static Map<String, String> descriptions = Stream.of(new String[][]{
            {"%helpSkinClear", Locale.HELP_SKIN_CLEAR},
            {"%helpSkinClearOther", Locale.HELP_SKIN_CLEAR_OTHER},
            {"%helpSkinUpdate", Locale.HELP_SKIN_UPDATE},
            {"%helpSkinUpdateOther", Locale.HELP_SKIN_UPDATE_OTHER},
            {"%helpSkinSet", Locale.HELP_SKIN_SET},
            {"%helpSkinSetOther", Locale.HELP_SKIN_SET_OTHER},
            {"%helpSkinSetUrl", Locale.HELP_SKIN_SET_OTHER_URL},
            {"%helpSrReload", Locale.HELP_SR_RELOAD},
            {"%helpSrStatus", Locale.HELP_SR_STATUS},
            {"%helpSrDrop", Locale.HELP_SR_DROP},
            {"%helpSrProps", Locale.HELP_SR_PROPS},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    public static Map<String, String> syntax = Stream.of(new String[][]{
            {"%SyntaxDefaultCommand", Locale.SYNTAX_DEFAULTCOMMAND},
            {"%SyntaxSkinSet", Locale.SYNTAX_SKINSET},
            {"%SyntaxSkinSetOther", Locale.SYNTAX_SKINSET_OTHER},
            {"%SyntaxSkinUrl", Locale.SYNTAX_SKINURL},
            {"%SyntaxSkinUpdateOther", Locale.SYNTAX_SKINUPDATE_OTHER},
            {"%SyntaxSkinClearOther", Locale.SYNTAX_SKINCLEAR_OTHER},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
}
