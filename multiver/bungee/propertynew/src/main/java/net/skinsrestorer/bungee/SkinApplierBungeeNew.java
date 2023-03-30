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

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;
import net.skinsrestorer.api.property.SkinProperty;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SkinApplierBungeeNew implements SkinApplyBungeeAdapter {
    @Override
    public void applyToHandler(InitialHandler handler, SkinProperty textures) {
        LoginResult profile = handler.getLoginProfile();
        Property[] newProps = new Property[]{new Property(SkinProperty.TEXTURES_NAME, textures.getValue(), textures.getSignature())};

        if (profile == null) {
            try {
                Field field = InitialHandler.class.getDeclaredField("loginProfile");
                field.setAccessible(true);
                field.set(handler, new LoginResult(null, null, newProps));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            profile.setProperties(newProps);
        }
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(ProxiedPlayer player) {
        Property[] props = ((InitialHandler) player.getPendingConnection()).getLoginProfile().getProperties();

        if (props == null) {
            return Optional.empty();
        }

        return Arrays.stream(props).filter(property -> property.getName().equals(SkinProperty.TEXTURES_NAME))
                .map(property -> SkinProperty.of(property.getValue(), property.getSignature())).findFirst();
    }
}
