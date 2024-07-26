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
package net.skinsrestorer.shared.subjects;

import ch.jalu.configme.SettingsManager;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import java.util.Locale;
import java.util.Optional;

@SuperBuilder
public abstract class AbstractSRCommandSender implements SRCommandSender {
    protected final @NonNull SettingsManager settings;
    protected final @NonNull SkinsRestorerLocale locale;

    @Override
    public Locale getLocale() {
        return settings.getProperty(MessageConfig.CONSOLE_LOCALE);
    }

    @Override
    public void sendMessage(Message key, TagResolver... resolvers) {
        Optional<ComponentString> translatedMessage = locale.getMessageOptional(this, key, resolvers);

        if (translatedMessage.isEmpty()) {
            return;
        }

        sendMessage(translatedMessage.get());
    }
}
