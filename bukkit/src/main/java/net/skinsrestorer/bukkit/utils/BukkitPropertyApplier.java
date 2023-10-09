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
package net.skinsrestorer.bukkit.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

public class BukkitPropertyApplier implements SkinApplyBukkitAdapter {
    @Override
    public void applyProperty(Player player, SkinProperty property) {
        try {
            GameProfile profile = getGameProfile(player, GameProfile.class);
            profile.getProperties().removeAll(SkinProperty.TEXTURES_NAME);
            profile.getProperties().put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(Player player) {
        try {
            Collection<Property> properties = getGameProfile(player, GameProfile.class).getProperties().values();

            return properties
                    .stream()
                    .filter(property -> property.getName().equals(SkinProperty.TEXTURES_NAME))
                    .map(this::convertToSRProperty)
                    .findFirst();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private SkinProperty convertToSRProperty(Property property) {
        try {
            return SkinProperty.of(property.getValue(), property.getSignature());
        } catch (NoSuchMethodError t) {
            // New authlib
            try {
                Method valueMethod = property.getClass().getMethod("value");
                Method signatureMethod = property.getClass().getMethod("signature");

                return SkinProperty.of((String) valueMethod.invoke(property), (String) signatureMethod.invoke(property));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
