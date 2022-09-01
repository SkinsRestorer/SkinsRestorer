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
package net.skinsrestorer.shared;

import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.*;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;
import net.skinsrestorer.shared.utils.DefaultForeignSubject;

public abstract class SkinsRestorerAPIShared extends SkinsRestorerAPI {
    private final DefaultForeignSubject defaultSubject = new DefaultForeignSubject();

    protected SkinsRestorerAPIShared(IMojangAPI mojangAPI, IMineSkinAPI mineSkinAPI, ISkinStorage skinStorage, IWrapperFactory wrapperFactory, IPropertyFactory propertyFactory) {
        super(mojangAPI, mineSkinAPI, skinStorage, wrapperFactory, propertyFactory);
    }

    public static SkinsRestorerAPIShared getApi() {
        return (SkinsRestorerAPIShared) SkinsRestorerAPI.getApi();
    }

    public abstract String getMessage(ISRForeign foreign, MessageKeyGetter key, Object... args);

    public ISRForeign getDefaultForeign() {
        return defaultSubject;
    }
}
