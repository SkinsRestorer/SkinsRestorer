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
    private static Map<String, String> newPermissions = Stream.of(new String[][]{
            {"skin", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command"},
            {"sr", "skinsrestorer.admincommand"},

            {"skins", "skinsrestorer.command.gui"},

            {"skinSet", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.set"},
            {"skinSetOther", "skinsrestorer.command.set.other"},

            {"skinClear", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.clear"},
            {"skinClearOther", "skinsrestorer.command.clear.other"},

            {"skinUpdate", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.command.update"},
            {"skinUpdateOther", "skinsrestorer.command.update.other"},

            {"srReload", "skinsrestorer.admincommand.reload"},
            {"srStatus", "skinsrestorer.admincommand.status"},
            {"srDrop", "skinsrestorer.admincommand.drop"},
            {"srProps", "skinsrestorer.admincommand.props"},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    private static Map<String, String> oldPermissions = Stream.of(new String[][]{
            {"skin", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.playercmds"},
            {"sr", "skinsrestorer.cmds"},

            {"skins", "skinsrestorer.playercmds.menu"},

            {"skinSet", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.playercmds"},
            {"skinSetOther", "skinsrestorer.cmds"},

            {"skinClear", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.playercmds"},
            {"skinClearOther", "skinsrestorer.cmds"},

            {"skinUpdate", Config.SKINWITHOUTPERM ? "" : "skinsrestorer.playercmds"},
            {"skinUpdateOther", "skinsrestorer.cmds"},

            {"srReload", "skinsrestorer.cmds"},
            {"srStatus", "skinsrestorer.cmds"},
            {"srDrop", "skinsrestorer.cmds"},
            {"srProps", "skinsrestorer.cmds"},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    public static Map<String, String> getPermissionReplacements() {
        return Config.USE_NEW_PERMISSIONS ? newPermissions : oldPermissions;
    }

    public static Map<String, String> descriptions = Stream.of(new String[][]{
            {"%helpSkinClear", Locale.HELP_SKIN_CLEAR},
            {"%helpSkinClearOther", Locale.HELP_SKIN_CLEAR_OTHER},
            {"%helpSkinUpdate", Locale.HELP_SKIN_UPDATE},
            {"%helpSkinUpdateOther", Locale.HELP_SKIN_UPDATE_OTHER},
            {"%helpSkinSet", Locale.HELP_SKIN_SET},
            {"%helpSkinSetOther", Locale.HELP_SKIN_SET_OTHER},
            {"%helpSrReload", Locale.HELP_SR_RELOAD},
            {"%helpSrStatus", Locale.HELP_SR_STATUS},
            {"%helpSrDrop", Locale.HELP_SR_DROP},
            {"%helpSrProps", Locale.HELP_SR_PROPS},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

}
