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
package net.skinsrestorer.bungee;

import ch.jalu.configme.SettingsManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SkinApplierBungeeNew extends SkinApplierBungeeShared {
    public SkinApplierBungeeNew(SettingsManager settings, SRLogger log) {
        super(settings, log);
    }

    protected void applyToHandler(InitialHandler handler, IProperty textures) {
        LoginResult profile = handler.getLoginProfile();
        Property[] newProps = new Property[]{(Property) textures.getHandle()};

        if (profile == null) {
            ReflectionUtil.setObject(InitialHandler.class, handler, "loginProfile",
                    new LoginResult(null, null, newProps));
        } else {
            profile.setProperties(newProps);
        }
    }

    @Override
    public List<IProperty> getProperties(ProxiedPlayer player) {
        Property[] props = ((InitialHandler) player.getPendingConnection()).getLoginProfile().getProperties();

        if (props == null) {
            return null;
        }

        return Arrays.stream(props).map(BungeePropertyNew::new).collect(Collectors.toList());
    }
}
