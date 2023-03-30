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
package net.skinsrestorer.v1_7;

import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.entity.Player;

import java.util.*;

public class BukkitLegacyPropertyApplier {
    public static void applyProperty(Player player, SkinProperty property) {
        try {
            GameProfile profile = getGameProfile(player);
            profile.getProperties().removeAll(SkinProperty.TEXTURES_NAME);
            profile.getProperties().put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static GameProfile getGameProfile(Player player) throws ReflectiveOperationException {
        Object ep = ReflectionUtil.invokeMethod(player.getClass(), player, "getHandle");
        return (GameProfile) ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
    }

    public static Optional<SkinProperty> getSkinProperty(Player player) {
        try {
            Collection<Property> getGameProfileProperties = getGameProfile(player).getProperties().values();

            return getGameProfileProperties
                    .stream()
                    .filter(property -> property.getName().equals(SkinProperty.TEXTURES_NAME))
                    .map(property -> SkinProperty.of(property.getValue(), property.getSignature()))
                    .findFirst();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
