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
package net.skinsrestorer.shared.subjects.messages;

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;

public class SkinsRestorerLocale {
    private final GsonComponentSerializer gsonSerializer = GsonComponentSerializer.gson();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Inject
    private LocaleManager localeManager;
    @Inject
    private SettingsManager settings;
    @Getter
    private final SRForeign defaultForeign = () -> settings.getProperty(MessageConfig.LOCALE);

    public String getMessage(SRForeign foreign, Message key) {
        return getMessage(foreign, key, TagResolver.empty());
    }

    public String getMessage(SRForeign foreign, Message key, TagResolver... tagResolver) {
        Component component = getMessageInternal(foreign, key, TagResolver.resolver(tagResolver))
                .orElseGet(Component::empty);

        return gsonSerializer.serialize(component);
    }

    public Optional<String> getMessageOptional(SRForeign foreign, Message key, TagResolver... tagResolver) {
        Optional<Component> component = getMessageInternal(foreign, key, TagResolver.resolver(tagResolver));

        return component.map(gsonSerializer::serialize);
    }

    private Optional<Component> getMessageInternal(SRForeign foreign, Message key, TagResolver tagResolver) {
        SRForeign target = settings.getProperty(MessageConfig.PER_ISSUER_LOCALE) ? foreign : defaultForeign;
        boolean isConsole = foreign instanceof SRCommandSender && !(foreign instanceof SRPlayer);
        Locale locale = isConsole ? settings.getProperty(MessageConfig.CONSOLE_LOCALE) : target.getLocale();

        String message = localeManager.getMessage(locale, key);

        if (message == null) {
            throw new IllegalStateException(String.format("Message %s not found", key.name()));
        }

        if (message.isEmpty()) {
            return Optional.empty();
        }

        Component component = miniMessage.deserialize(message, tagResolver);

        Message parent = key.getParent();
        if (parent != null && (parent != Message.PREFIX_FORMAT || !settings.getProperty(MessageConfig.DISABLE_PREFIX))) {
            return getMessageInternal(target, Message.PREFIX_FORMAT, TagResolver.resolver(
                    tagResolver,
                    Placeholder.component("message", component)
            ));
        } else {
            return Optional.of(component);
        }
    }
}
