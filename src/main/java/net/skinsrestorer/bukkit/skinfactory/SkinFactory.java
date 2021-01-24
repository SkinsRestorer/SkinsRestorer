/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bukkit.skinfactory;

import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public interface SkinFactory {
    /**
     * Applies the skin In other words, sets the skin data, but no changes will
     * be visible until you reconnect or force update with
     *
     * @param p     - Player
     * @param props - Property Object
     */
    default void applySkin(final Player p, Object props) {
        // delay 1 servertick so we override online-mode
        Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
            try {
                if (props == null)
                    return;

                Object ep = ReflectionUtil.invokeMethod(p.getClass(), p, "getHandle");
                Object profile = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
                Object propMap = ReflectionUtil.invokeMethod(profile.getClass(), profile, "getProperties");
                ReflectionUtil.invokeMethod(propMap, "clear");
                ReflectionUtil.invokeMethod(propMap.getClass(), propMap, "put", new Class[]{Object.class, Object.class}, "textures", props);

                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> updateSkin(p));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Instantly updates player's skin
     *
     * @param p - Player
     */
    void updateSkin(Player p);
}
