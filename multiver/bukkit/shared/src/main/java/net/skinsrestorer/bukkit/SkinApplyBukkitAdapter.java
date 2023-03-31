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
package net.skinsrestorer.bukkit;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface SkinApplyBukkitAdapter {
    void applyProperty(Player player, SkinProperty property);

    Optional<SkinProperty> getSkinProperty(Player player);

    default <G> G getGameProfile(Player player, Class<G> gClass) throws ReflectiveOperationException {
        Object entityPlayer = ReflectionUtil.invokeMethod(player.getClass(), player, "getHandle");
        try {
            return gClass.cast(ReflectionUtil.invokeMethod(entityPlayer.getClass(), entityPlayer, "getProfile"));
        } catch (ReflectiveOperationException e) {
            return gClass.cast(ReflectionUtil.getFieldByType(entityPlayer, "GameProfile"));
        }
    }
}
