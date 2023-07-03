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
package net.skinsrestorer.shared.exception;

import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.ComponentHelper;

public class MineSkinExceptionShared extends MineSkinException implements TranslatableException {
    private final Message message;
    private final Object[] args;

    public MineSkinExceptionShared(SkinsRestorerLocale locale, Message message, Object... args) {
        super(ComponentHelper.convertJsonToPlain(locale.getMessage(locale.getDefaultForeign(), message, args)));
        this.message = message;
        this.args = args;
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
    public String getMessage(SRForeign foreign, SkinsRestorerLocale locale) {
        return locale.getMessage(foreign, message, args);
    }
}
