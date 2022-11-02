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

import co.aikar.locales.LocaleManager;
import co.aikar.locales.MessageKey;
import lombok.Getter;
import lombok.SneakyThrows;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;
import net.skinsrestorer.shared.utils.LocaleParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public enum Message implements MessageKeyGetter {
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
    HELP_SKULL_GET,
    HELP_SKULL_GIVE,
    HELP_SKULL_UPDATE,
    HELP_SKULL_PROPS,
    SYNTAX_DEFAULTCOMMAND,
    SYNTAX_SKINSET,
    SYNTAX_SKINSET_OTHER,
    SYNTAX_SKINURL,
    SYNTAX_SKINSEARCH,
    SYNTAX_SKINUPDATE_OTHER,
    SYNTAX_SKINCLEAR_OTHER,
    SYNTAX_SKULL_GET,
    SYNTAX_SKULL_GIVE,
    COMPLETIONS_SKIN,
    COMPLETIONS_SKINNAME,
    COMPLETIONS_SKINURL,
    COMMAND_SERVER_NOT_ALLOWED_MESSAGE,
    PLAYER_HAS_NO_PERMISSION_SKIN,
    PLAYER_HAS_NO_PERMISSION_URL,
    SKIN_DISABLED,
    SKINURL_DISALLOWED,
    NOT_PREMIUM,
    INVALID_PLAYER,
    SKIN_COOLDOWN,
    SKIN_CHANGE_SUCCESS,
    SKIN_CLEAR_SUCCESS,
    SKIN_CLEAR_ISSUER,
    MS_UPDATING_SKIN,
    SUCCESS_CREATE_SKIN,
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
    ADMIN_SET_SKIN,
    DATA_DROPPED,
    ADMIN_APPLYSKIN_SUCCES,
    ADMIN_APPLYSKIN_ERROR,
    STATUS_OK,
    ALT_API_FAILED,
    MS_API_FAILED,
    NO_SKIN_DATA,
    RELOAD,
    OUTDATED,
    SR_LINE,
    CUSTOM_HELP_IF_ENABLED;

    @Getter
    private final MessageKey key = MessageKey.of("skinsrestorer." + this.name().toLowerCase());

    @SneakyThrows
    public static void load(LocaleManager<ISRForeign> manager, Path dataFolder, ISRPlugin plugin) {
        Path languagesFolder = dataFolder.resolve("languages");
        Files.createDirectories(languagesFolder);
        CodeSource src = Message.class.getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jar = src.getLocation();
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            List<Locale> locales = new ArrayList<>();
            while (true) {
                ZipEntry e = zip.getNextEntry();
                if (e == null)
                    break;

                String name = e.getName();
                if (name.startsWith("languages/language") && name.endsWith(".properties")) {
                    String fileName = name.replace("languages/", "");
                    if (fileName.startsWith("language_")) {
                        locales.add(LocaleParser.parseLocaleStrict(fileName.replace("language_", "").replace(".properties", "")));
                    }
                    if (!Files.exists(languagesFolder.resolve(fileName))) {
                        try (InputStream is = plugin.getResource(name)) {
                            Files.copy(is, languagesFolder.resolve(fileName));
                        }
                    }
                }
            }

            manager.addMessageBundle("languages.language", locales.toArray(new Locale[0]));

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(languagesFolder)) {
                for (Path path : stream) {
                    String fileName = path.getFileName().toString();
                    if (fileName.equals("language.properties")) {
                        try (InputStream in = Files.newInputStream(path)) {
                            PropertyResourceBundle bundle = new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
                            manager.addResourceBundle(bundle, manager.getDefaultLocale());
                        }
                    } else if (fileName.startsWith("language_") && fileName.endsWith(".properties")) {
                        Locale locale = LocaleParser.parseLocaleStrict(fileName.replace("language_", "").replace(".properties", ""));
                        try (InputStream in = Files.newInputStream(path)) {
                            PropertyResourceBundle bundle = new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
                            manager.addResourceBundle(bundle, locale);
                        }
                    }
                }
            }
        } else {
            throw new IOException("Could not find default language files");
        }
    }
}
