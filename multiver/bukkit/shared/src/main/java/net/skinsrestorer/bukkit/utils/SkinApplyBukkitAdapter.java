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
package net.skinsrestorer.bukkit.utils;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface SkinApplyBukkitAdapter {
    void applyProperty(Player player, SkinProperty property);

    Optional<SkinProperty> getSkinProperty(Player player);

    default <G> G getGameProfile(Player player, Class<G> gClass) throws ReflectiveOperationException {
        Object serverPlayer = HandleReflection.getHandle(player, Object.class);

        Object profile;
        try {
            profile = ReflectionUtil.invokeObjectMethod(serverPlayer, "getProfile");
        } catch (ReflectiveOperationException e) {
            profile = ReflectionUtil.getFieldByType(serverPlayer, "GameProfile");
        }

        return gClass.cast(profile);
    }
}
