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

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import net.skinsrestorer.shadow.kyori.adventure.text.Component;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.MiniMessage;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRForeign;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;

public class SkinsRestorerLocale {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    @Getter
    private final SRForeign englishForeign = () -> Locale.ENGLISH;
    @Inject
    private LocaleManager localeManager;
    @Inject
    private SettingsManager settings;
    @Getter
    private final SRForeign defaultForeign = () -> settings.getProperty(MessageConfig.LOCALE);

    public ComponentString getMessageRequired(SRForeign foreign, Message key, TagResolver... tagResolver) {
        return ComponentHelper.convertComponentToJson(getMessageInternal(foreign, key, TagResolver.resolver(tagResolver))
                .orElseGet(Component::empty));
    }

    public Optional<ComponentString> getMessageOptional(SRForeign foreign, Message key, TagResolver... tagResolver) {
        return getMessageInternal(foreign, key, TagResolver.resolver(tagResolver))
                .map(ComponentHelper::convertComponentToJson);
    }

    private Optional<Component> getMessageInternal(SRForeign foreign, Message key, TagResolver tagResolver) {
        String message = localeManager.getMessage(foreign.getLocale(), key);

        if (message == null) {
            throw new IllegalStateException("Message %s not found".formatted(key.name()));
        }

        if (message.isEmpty()) {
            return Optional.empty();
        }

        Component component = miniMessage.deserialize(message, tagResolver);
        Message parent = key.getParent();
        if (parent != null && (parent != Message.PREFIX_FORMAT || !settings.getProperty(MessageConfig.DISABLE_PREFIX))) {
            return getMessageInternal(foreign, parent, TagResolver.resolver(
                    tagResolver,
                    Placeholder.component("message", component)
            ));
        } else {
            return Optional.of(component);
        }
    }
}
