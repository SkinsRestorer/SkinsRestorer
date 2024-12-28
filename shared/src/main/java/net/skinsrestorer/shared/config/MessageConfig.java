/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
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
            "A locale code for the locale you want to use by default for messages and commands.",
            "Has to be a string separated by an underscore."
    })
    public static final Property<Locale> LOCALE = newLocaleProperty("messages.locale", Locale.ENGLISH);
    @Comment({
            "A locale code for the messages and commands sent to the console.",
            "This is useful if you want to use a different locale for the console than for players.",
            "We recommend keeping this at the default value because we mostly only provide support in English.",
            "Has to be a string separated by an underscore."
    })
    public static final Property<Locale> CONSOLE_LOCALE = newLocaleProperty("messages.consoleLocale", Locale.ENGLISH);
    @Comment("Disable the message prefix in SkinsRestorer messages.")
    public static final Property<Boolean> DISABLE_PREFIX = newProperty("messages.disablePrefix", false);
    @Comment({
            "Every message sent by the plugin will use the players client locale if a translation is available.",
            "If disabled, the config locale will be used instead."
    })
    public static final Property<Boolean> PER_ISSUER_LOCALE = newProperty("messages.perIssuerLocale", true);
    @Comment({
            "How dates are formatted by the plugin in messages. Format is explained here:",
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
                "Translation & message options here",
                "To learn more about translations and how to make custom translations, visit: https://skinsrestorer.net/docs/configuration/locale-translations"
        );
    }
}
