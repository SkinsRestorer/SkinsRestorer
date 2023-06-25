/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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

import com.google.gson.JsonElement;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.skinsrestorer.shared.exception.TranslatableException;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

public class ComponentHelper {
    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();

    public static JsonElement parse(String s) {
        return GSON_COMPONENT_SERIALIZER.serializeToTree(LEGACY_COMPONENT_SERIALIZER.deserialize(s));
    }

    public static String convertToPlain(JsonElement jsonElement) {
        return PLAIN_COMPONENT_SERIALIZER.serialize(GSON_COMPONENT_SERIALIZER.deserializeFromTree(jsonElement));
    }

    public static void sendException(Throwable t, SRCommandSender sender, SkinsRestorerLocale locale) {
        if (t instanceof TranslatableException) {
            sender.sendMessage(((TranslatableException) t).getMessage(sender, locale));
        } else {
            sender.sendMessage(parse(SharedMethods.getRootCause(t).getMessage()));
        }
    }
}
