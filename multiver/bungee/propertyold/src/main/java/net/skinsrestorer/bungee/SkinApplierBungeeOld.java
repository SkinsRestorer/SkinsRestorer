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
package net.skinsrestorer.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.utils.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

public class SkinApplierBungeeOld implements SkinApplyBungeeAdapter {
    @Override
    public void applyToHandler(InitialHandler handler, SkinProperty property) throws ReflectiveOperationException {
        LoginResult profile = handler.getLoginProfile();
        Property[] newProps = new Property[]{new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature())};

        if (profile == null) {
            try {
                // NEW BUNGEECORD (id, name, property)
                profile = new LoginResult(null, null, newProps);
            } catch (Exception ignored) {
                // FALL BACK TO OLD (id, property)
                profile = (LoginResult) ReflectionUtil.invokeConstructor(LoginResult.class,
                        new Class<?>[]{String.class, Property[].class},
                        null, newProps);
            }

            try {
                Field field = InitialHandler.class.getDeclaredField("loginProfile");
                field.setAccessible(true);
                field.set(handler, profile);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to apply skin property to InitialHandler", e);
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

        return Arrays.stream(props)
                .map(property -> SkinProperty.tryParse(
                        property.getName(),
                        property.getValue(),
                        property.getSignature()
                ))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
