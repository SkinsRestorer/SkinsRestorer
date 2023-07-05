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
package net.skinsrestorer.shared.subjects;

import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.ComponentHelper;

import java.util.Optional;

@SuperBuilder
public abstract class AbstractSRCommandSender implements SRCommandSender {
    public void sendMessage(Message key, TagResolver... resolvers) {
        Optional<String> translatedMessage = getSRLocale().getMessageOptional(this, key, resolvers);

        if (!translatedMessage.isPresent()) {
            return;
        }

        sendMessage(translatedMessage.get());
    }

    protected abstract SkinsRestorerLocale getSRLocale();
}
