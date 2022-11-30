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

import lombok.Getter;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.interfaces.ISRServerPlugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Getter
public abstract class SkinsRestorerServerShared extends SkinsRestorerShared implements ISRServerPlugin {
    protected boolean proxyMode;

    protected SkinsRestorerServerShared(ISRLogger isrLogger, boolean loggerColor, String version, String updateCheckerAgent, Path dataFolder) {
        super(isrLogger, loggerColor, version, updateCheckerAgent, dataFolder);
        injector.register(ISRServerPlugin.class, this);
    }

    /**
     * Legacy method of converting SkinGUI plugin message data to a map.
     *
     * @deprecated Use {@link #convertToObjectV2(byte[])} instead. Will be removed in 15.0.0
     */
    @SuppressWarnings("unchecked")
    protected static Map<String, GenericProperty> convertToObject(byte[] byteArr) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArr);
            ObjectInputStream ois = new ObjectInputStream(bis);

            return (Map<String, GenericProperty>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
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
