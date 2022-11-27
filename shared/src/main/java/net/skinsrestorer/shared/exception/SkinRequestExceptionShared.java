/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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

import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;

public class SkinRequestExceptionShared extends SkinRequestException {
    public SkinRequestExceptionShared() {
        super();
    }

    public SkinRequestExceptionShared(String message) {
        super(message);
    }

    public SkinRequestExceptionShared(Throwable e) {
        super(e);
    }

    public SkinRequestExceptionShared(MessageKeyGetter key, Object... args) {
        this(SkinsRestorerAPIShared.getApi().getMessage(SkinsRestorerAPIShared.getDefaultForeign(), key, args));
    }
}
