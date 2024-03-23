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
package net.skinsrestorer.shared.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.skinsrestorer.shared.exception.TranslatableException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import java.util.List;
import java.util.Optional;

public class ComponentHelper {
    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI_MESSAGE_COMPONENT_SERIALIZER = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();

    public static ComponentString parseMiniMessageToJsonString(String miniMessage) {
        return new ComponentString(GSON_COMPONENT_SERIALIZER.serialize(MINI_MESSAGE_COMPONENT_SERIALIZER.deserialize(miniMessage)));
    }

    // Only used on platforms that don't support adventure
    public static String convertJsonToLegacy(ComponentString messageJson) {
        return LEGACY_COMPONENT_SERIALIZER.serialize(GSON_COMPONENT_SERIALIZER.deserialize(messageJson.jsonString()));
    }

    public static ComponentString convertToJsonString(Component component) {
        return new ComponentString(GSON_COMPONENT_SERIALIZER.serialize(component));
    }

    public static Component convertJsonToComponent(ComponentString messageJson) {
        return GSON_COMPONENT_SERIALIZER.deserialize(messageJson.jsonString());
    }

    public static String convertToPlain(Component component) {
        return PLAIN_COMPONENT_SERIALIZER.serialize(component);
    }

    public static ComponentString joinNewline(List<ComponentString> components) {
        return convertToJsonString(Component.join(JoinConfiguration.newlines(),
                components.stream().map(ComponentHelper::convertJsonToComponent).toList()));
    }

    public static void sendException(Throwable t, SRCommandSender sender, SkinsRestorerLocale locale, SRLogger logger) {
        Optional<ComponentString> message;
        if (t instanceof TranslatableException exception) {
            message = exception.getMessageOptional(sender, locale);
        } else {
            logger.warning("An unexpected error occurred while executing a command", t);

            message = locale.getMessageOptional(sender, Message.ERROR_GENERIC,
                    Placeholder.unparsed("message", SRHelpers.getRootCause(t).getMessage()));
        }

        message.ifPresent(sender::sendMessage);
    }
}
