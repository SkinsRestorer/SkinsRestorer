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
package net.skinsrestorer.shared.exception;

import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import java.util.Optional;

public class MineSkinExceptionShared extends MineSkinException implements TranslatableException {
    private final Message message;
    private final TagResolver[] args;

    public MineSkinExceptionShared(Message message, TagResolver... resolvers) {
        // Not important since we use #getMessage(SRForeign, SkinsRestorerLocale) to get the message
        super(message.toString());
        this.message = message;
        this.args = resolvers;
    }

    public MineSkinExceptionShared(MineSkinException cause) {
        this((MineSkinExceptionShared) cause);
    }

    public MineSkinExceptionShared(MineSkinExceptionShared cause) {
        super(cause);
        this.message = cause.message;
        this.args = cause.args;
    }

    @Override
    public Optional<ComponentString> getMessageOptional(SRForeign foreign, SkinsRestorerLocale locale) {
        return locale.getMessageOptional(foreign, message, args);
    }
}
