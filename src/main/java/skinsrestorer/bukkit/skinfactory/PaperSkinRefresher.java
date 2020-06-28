package skinsrestorer.bukkit.skinfactory;

import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.entity.Player;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

/**
 * https://github.com/SkinsRestorer/SkinsRestorerX/pull/240
 */
final class PaperSkinRefresher implements Consumer<Player> {
    private static final MethodHandle MH_REFRESH;
    private static MethodHandle MH_HEALTH_UPDATE = null;

    @Override
    @SneakyThrows
    public void accept(Player player) {
        MH_REFRESH.invoke(player);

        if (MH_HEALTH_UPDATE != null)
            MH_HEALTH_UPDATE.invoke(player);
    }

    static {
        try {
            val field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            MethodHandles.publicLookup();
            val lookup = (MethodHandles.Lookup) field.get(null);
            MH_REFRESH = lookup.findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "refreshPlayer", MethodType.methodType(Void.TYPE));

            // XP won't get updated on unsupported Paper builds
            try {
                MH_HEALTH_UPDATE = lookup.findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "triggerHealthUpdate", MethodType.methodType(Void.TYPE));
            } catch (Exception ignored) {
                try {
                    MH_HEALTH_UPDATE = lookup.findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "resetMaxHealth", MethodType.methodType(Void.TYPE));
                } catch (Exception ignored2) {
                }
            }
            System.out.println("[SkinsRestorer] Using PaperSkinRefresher");
        } catch (Exception e) {
            System.out.println("[SkinsRestorer] Failed PaperSkinRefresher");
            throw new ExceptionInInitializerError(e);
        }
    }
}