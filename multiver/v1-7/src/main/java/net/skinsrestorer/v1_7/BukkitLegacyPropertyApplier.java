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
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.reflection.exception.ReflectionException;
import org.bukkit.entity.Player;

import java.util.*;

public class BukkitLegacyPropertyApplier {
    public static void applyProperty(Player player, IProperty property) {
        try {
            GameProfile profile = getGameProfile(player);
            profile.getProperties().removeAll(IProperty.TEXTURES_NAME);
            profile.getProperties().put(IProperty.TEXTURES_NAME, (Property) property.getHandle());
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    public static GameProfile getGameProfile(Player player) throws ReflectionException {
        Object ep = ReflectionUtil.invokeMethod(player.getClass(), player, "getHandle");
        return (GameProfile) ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
    }

    public static Map<String, Collection<IProperty>> getPlayerProperties(Player player) {
        try {
            Map<String, Collection<Property>> getGameProfileProperties = getGameProfile(player).getProperties().asMap();

            Map<String, Collection<IProperty>> properties = new HashMap<>();
            for (Map.Entry<String, Collection<Property>> entry : getGameProfileProperties.entrySet()) {
                List<IProperty> list = new ArrayList<>();

                for (Property property : entry.getValue()) {
                    list.add(new BukkitLegacyProperty(property.getName(), property.getValue(), property.getSignature()));
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
