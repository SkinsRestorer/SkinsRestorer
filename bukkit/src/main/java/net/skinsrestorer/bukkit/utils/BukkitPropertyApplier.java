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
package net.skinsrestorer.bukkit.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.reflection.exception.ReflectionException;
import org.bukkit.entity.Player;

import java.util.*;

public class BukkitPropertyApplier {
    @SuppressWarnings("unchecked")
    public static void applyProperty(Player player, SkinProperty property) {
        try {
            GameProfile profile = getGameProfile(player);
            profile.getProperties().removeAll(SkinProperty.TEXTURES_NAME);
            profile.getProperties().put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    public static GameProfile getGameProfile(Player player) throws ReflectionException {
        Object ep = ReflectionUtil.invokeMethod(player.getClass(), player, "getHandle");
        GameProfile profile;
        try {
            profile = (GameProfile) ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
        } catch (Exception e) {
            profile = (GameProfile) ReflectionUtil.getFieldByType(ep, "GameProfile");
        }

        return profile;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Collection<SkinProperty>> getPlayerProperties(Player player) {
        try {
            Map<String, Collection<Property>> getGameProfileProperties = getGameProfile(player).getProperties().asMap();

            Map<String, Collection<SkinProperty>> properties = new HashMap<>();
            for (Map.Entry<String, Collection<Property>> entry : getGameProfileProperties.entrySet()) {
                List<SkinProperty> list = new ArrayList<>();

                for (Property property : entry.getValue()) {
                    list.add(SkinProperty.of(property.getValue(), property.getSignature()));
                }

                properties.put(entry.getKey(), list);
            }

            return properties;
        } catch (ReflectionException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}
