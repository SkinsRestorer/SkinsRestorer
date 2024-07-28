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

import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.Objects;

public class SkullUtil {
    public static void setSkull(ItemStack skullItem, String hash) {
        ItemMeta skullMeta = Objects.requireNonNull(skullItem.getItemMeta());

        try {
            Field profileField = Objects.requireNonNull(skullMeta.getClass().getDeclaredField("profile"));
            profileField.setAccessible(true);
            profileField.set(skullMeta, Profileable.of(Objects.requireNonNull(ProfileInputType.typeOf(hash)), hash).getProfile());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        skullItem.setItemMeta(skullMeta);
    }
}
