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
package net.skinsrestorer.shared.subjects.messages;

import net.skinsrestorer.shadow.kyori.adventure.audience.Audience;
import net.skinsrestorer.shadow.kyori.adventure.text.Component;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.MiniMessage;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.skinsrestorer.shadow.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.skinsrestorer.shadow.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.shadow.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.skinsrestorer.shared.exception.TranslatableException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ComponentHelper {
    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI_MESSAGE_COMPONENT_SERIALIZER = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();

    public static ComponentString parseMiniMessageToJsonString(String miniMessage, TagResolver... resolvers) {
        return new ComponentString(GSON_COMPONENT_SERIALIZER.serialize(MINI_MESSAGE_COMPONENT_SERIALIZER.deserialize(miniMessage, TagResolver.resolver(resolvers))));
    }

    // Only used on platforms that don't support adventure
    public static String convertJsonToLegacy(ComponentString messageJson) {
        return LEGACY_COMPONENT_SERIALIZER.serialize(GSON_COMPONENT_SERIALIZER.deserialize(messageJson.jsonString()));
    }

    public static ComponentString convertComponentToJson(Component component) {
        return new ComponentString(GSON_COMPONENT_SERIALIZER.serialize(component));
    }

    public static Component convertJsonToComponent(ComponentString messageJson) {
        return GSON_COMPONENT_SERIALIZER.deserialize(messageJson.jsonString());
    }

    public static String convertComponentToPlain(Component component) {
        return PLAIN_COMPONENT_SERIALIZER.serialize(component);
    }

    public static ComponentString convertPlainToJson(String text) {
        return convertComponentToJson(PLAIN_COMPONENT_SERIALIZER.deserialize(text));
    }

    public static String convertJsonToPlain(ComponentString messageJson) {
        return convertComponentToPlain(convertJsonToComponent(messageJson));
    }

    public static Audience commandSenderToAudience(SRCommandSender sender) {
        return new Audience() {
            @Override
            public void sendMessage(@NotNull Component message) {
                sender.sendMessage(convertComponentToJson(message));
            }
        };
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
