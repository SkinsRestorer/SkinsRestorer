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
package net.skinsrestorer.shared.platform;

import lombok.Getter;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.shared.interfaces.SRPlatformLogger;
import net.skinsrestorer.shared.interfaces.SRServerPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Getter
public abstract class SkinsRestorerServerPlatform extends SkinsRestorerPlatform implements SRServerPlugin {
    protected boolean proxyMode;

    protected SkinsRestorerServerPlatform(SRPlatformLogger isrLogger, boolean loggerColor, String version, String updateCheckerAgent, Path dataFolder,
                                          IWrapperFactory wrapperFactory, IPropertyFactory propertyFactory, Platform platform) {
        super(isrLogger, loggerColor, version, updateCheckerAgent, dataFolder, wrapperFactory, propertyFactory, platform);
        injector.register(SRServerPlugin.class, this);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, String> convertToObjectV2(byte[] byteArr) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            return (Map<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}
