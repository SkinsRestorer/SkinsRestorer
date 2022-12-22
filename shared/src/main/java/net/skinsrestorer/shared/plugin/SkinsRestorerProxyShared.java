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
package net.skinsrestorer.shared.plugin;

import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;

import java.nio.file.Path;

public abstract class SkinsRestorerProxyShared extends SkinsRestorerShared implements ISRProxyPlugin {
    protected SkinsRestorerProxyShared(ISRLogger isrLogger, boolean loggerColor, String version, String updateCheckerAgent, Path dataFolder, IWrapperFactory wrapperFactory, IPropertyFactory propertyFactory) {
        super(isrLogger, loggerColor, version, updateCheckerAgent, dataFolder, wrapperFactory, propertyFactory);
        injector.register(ISRProxyPlugin.class, this);
    }

    @Override
    protected boolean isProxyMode() {
        return false;
    }
}
