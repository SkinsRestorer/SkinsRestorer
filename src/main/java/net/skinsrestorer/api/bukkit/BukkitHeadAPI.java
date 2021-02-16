package net.skinsrestorer.api.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;
import java.util.UUID;

public class BukkitHeadAPI {
    public static void setSkull(ItemStack head, String b64stringTexture) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        PropertyMap propertyMap = profile.getProperties();

        if (propertyMap == null) {
            throw new IllegalStateException("Profile doesn't contain a property map");
        }

        propertyMap.put("textures", new Property("textures", b64stringTexture));

        ItemMeta headMeta = head.getItemMeta();
        Class<?> headMetaClass = Objects.requireNonNull(headMeta).getClass();

        try {
            ReflectionUtil.getField(headMetaClass, "profile", GameProfile.class, 0).set(headMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

        head.setItemMeta(headMeta);
    }
}
