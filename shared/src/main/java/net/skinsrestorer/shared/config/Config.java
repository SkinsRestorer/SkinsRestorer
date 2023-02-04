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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.TypeBasedProperty;
import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import net.skinsrestorer.shared.utils.LocaleParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.utils.FluentList.listOf;

public class Config implements SettingsHolder {
    @Comment({
            "Stops the process of setting a skin if the LoginEvent was canceled by an AntiBot plugin.",
            "[?] Unsure? leave this true for better performance."
    })
    public static final Property<Boolean> NO_SKIN_IF_LOGIN_CANCELED = newProperty("NoSkinIfLoginCanceled", true);
    @Comment("This will make SkinsRestorer always apply the skin even if the player joins as premium on an online mode server.")
    public static final Property<Boolean> ALWAYS_APPLY_PREMIUM = newProperty("AlwaysApplyPremium", false);
    @Comment({
            "\n###############",
            "\n# Danger Zone #",
            "\n###############",
            "\n",
            "ABSOLUTELY DO NOT CHANGE IF YOU DON'T KNOW WHAT YOU DO",
            "\n",
            "<!! Warning !!>",
            "Enabling this will stop SkinsRestorer to change skins on join.",
            "Handy for when you want run /skin apply to apply skin after texturepack popup"
    })
    public static final Property<Boolean> DISABLE_ON_JOIN_SKINS = newProperty("DisableOnJoinSkins", false);
    @Comment({
            "<!! Warning Experimental !!>",
            "This enables the experimental PaperMC join event integration that allows instant skins on join.",
            "It is not as tested as the default implementation, but it is smoother and should not lag the server.",
            "It also fixes all resource pack skin apply issues.",
            "If your players are experiencing extremely long loading screens, try disabling this."
    })
    public static final Property<Boolean> ENABLE_PAPER_JOIN_LISTENER = newProperty("EnablePaperJoinListener", true);
    @Comment({
            "<!! Warning !!>",
            "When enabled if a skin gets applied on the proxy, the new texture will be forwarded to the backend as well.",
            "This is optional sometimes as the backend may pick up the new one of the proxy.",
            "It is recommended though to **KEEP THIS ON** because it keeps the backend data in sync.",
            "This feature is required for solutions like RedisBungee and also fixes bugs in some cases."
    })
    public static final Property<Boolean> FORWARD_TEXTURES = newProperty("ForwardTextures", true);
    @Comment({
            "Updater Settings",
            "<!! Warning !!>",
            "Using outdated version void's support, compatibility & stability.",
            "\n",
            "To block all types of automatic updates (which can risk keeping an exploit):",
            "Create a file called 'noupdate.txt' in the plugin directory (./plugins/SkinsRestorer/ )",
            "\n",
            "\n################",
            "\n# DEV's corner #",
            "\n################",
            "\n",
            "Enable these on the dev's request",
            "\n",
            "Enable to start receiving debug messages about api requests & more."
    })
    public static final Property<Boolean> DEBUG = newProperty("Debug", false);

    private Config() {
    }

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("",
                "\n##################################",
                "\n#      SkinsRestorer config      #",
                "\n##################################",
                "\n",
                "We from SRTeam thank you for using our plugin!",
                "For more information        -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/",
                "For installation            -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Installing-SkinsRestorer",
                "For Configuration Help      -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Configuration",
                "Commands & Permissions      -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/cmds-&-perms",
                "For locale & messages       -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Locale-and-Translations",
                "Not working or get error?  -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Troubleshoot",
                "For advanced help or other, go to our Discord: https://discord.me/SkinsRestorer/",
                "\n",
                "(?) update config? -> https://raw.githubusercontent.com/SkinsRestorer/SkinsRestorerX/stable/shared/src/main/resources/config.yml",
                "(?) Step by step config guide -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Configuration",
                "\n",
                "(!) IF YOU ARE USING A PROXY (Bungee, Waterfall or Velocity), Check & set on every BACKEND server spigot.yml -> bungeecord: true.  (!)",
                "(!) & Install Skinsrestorer.jar on ALL SERVERS!!! (BOTH Backend & Proxy).                      (!)"
        );
        conf.setFooter(
                "\n",
                "\n# End #",
                "\n",
                "Useful tools:",
                "Vectier Thailand has made some super cool \"Custom Skin\" tools that you can use!",
                "",
                "SkinFile Generator:",
                "With SkinFile Generator, you can upload your own custom skin to get a unique .skin file that you can put in your skins folder, to use with SkinsRestorer.",
                "Check it out here: https://skinsrestorer.github.io/SkinFile-Generator/",
                "",
                "SkinSystem :",
                "With SkinSystem, you, as a server owner, can connect AuthMe (and forum) with the SkinSystem website that you can host, to give your players the ability to upload custom skins.",
                "Check it out here: https://github.com/SkinsRestorer/SkinSystem",
                "",
                "\n# Useful Links #",
                "Website: https://skinsrestorer.net/",
                "Download: https://github.com/SkinsRestorer/SkinsRestorerX/releases",
                "Wiki https://github.com/SkinsRestorer/SkinsRestorerX/wiki/",
                "Spigot: https://www.spigotmc.org/resources/skinsrestorer.2124/",
                "Github: https://github.com/SkinsRestorer/SkinsRestorerX/",
                "Discord: https://discord.me/SkinsRestorer/"
        );
    }
}
