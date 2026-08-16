/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import java.util.Locale;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newLocaleProperty;

public class MessageConfig implements SettingsHolder {
    @Comment({
            "The default locale code for player messages and commands.",
            "Use an underscore between the language and country codes. Example: en_US"
    })
    public static final Property<Locale> LOCALE = newLocaleProperty("messages.locale", Locale.ENGLISH);
    @Comment({
            "The locale code for console messages and commands.",
            "This value can differ from the player locale.",
            "Support is primarily available in English, so the default value is recommended.",
            "Use an underscore between the language and country codes. Example: en_US"
    })
    public static final Property<Locale> CONSOLE_LOCALE = newLocaleProperty("messages.consoleLocale", Locale.ENGLISH);
    @Comment("Removes the SkinsRestorer prefix from messages.")
    public static final Property<Boolean> DISABLE_PREFIX = newProperty("messages.disablePrefix", false);
    @Comment({
            "Uses each player's client locale when a translation is available.",
            "When disabled, all players use messages.locale."
    })
    public static final Property<Boolean> PER_ISSUER_LOCALE = newProperty("messages.perIssuerLocale", true);
    @Comment({
            "The date format for plugin messages. Format reference:",
            "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html"
    })
    public static final Property<String> DATE_FORMAT = newProperty("messages.dateFormat", "dd MMMM yyyy");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("messages",
                "\n",
                "\n##########",
                "\n# Locale #",
                "\n##########",
                "\n",
                "Controls message languages and formatting.",
                "Translation guide: https://skinsrestorer.net/docs/configuration/locale-translations"
        );
    }
}
