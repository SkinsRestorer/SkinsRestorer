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
