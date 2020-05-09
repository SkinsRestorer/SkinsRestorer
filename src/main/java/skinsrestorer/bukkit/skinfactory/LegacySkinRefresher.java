package skinsrestorer.bukkit.skinfactory;

import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

/**
 * Credits to xxDark
 * https://github.com/SkinsRestorer/SkinsRestorerX/pull/240
 * Currently not working because it relies on paperspigot-only methods
 */
final class LegacySkinRefresher implements Consumer<Player> {
    private static final MethodHandle MH_GET_HANDLE;
    private static final MethodHandle MH_REREGISTER;
    private static final MethodHandle MH_GET_WORLD_HANDLE;
    private static final MethodHandle MH_WORLD_DIMENSION;
    private static final Object PLAYER_LIST;
    private static final MethodHandle MH_MOVE_TO_WORLD;

    @Override
    @SneakyThrows
    public void accept(Player player) {
        val vehicle = player.getVehicle();
        val handle = MH_GET_HANDLE.invoke(player);
        val location = player.getLocation();
        val world = location.getWorld();
        val dimension = MH_WORLD_DIMENSION.invoke(MH_GET_WORLD_HANDLE.invoke(world));
        MH_REREGISTER.invoke(player, handle);
        MH_MOVE_TO_WORLD.invoke(PLAYER_LIST, handle, dimension, false, location, false);
        if (vehicle != null) {
            vehicle.addPassenger(player);
        }
    }

    static {
        try {
            val field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            MethodHandles.publicLookup();
            val lookup = (MethodHandles.Lookup) field.get(null);
            val craftPlayerClass = ReflectionUtil.getBukkitClass("entity.CraftPlayer");
            val entityPlayerClass = ReflectionUtil.getNMSClass("EntityPlayer");
            MH_GET_HANDLE = lookup.findVirtual(craftPlayerClass, "getHandle", MethodType.methodType(entityPlayerClass));
            MH_REREGISTER = lookup.findVirtual(craftPlayerClass, "reregisterPlayer", MethodType.methodType(Void.TYPE, entityPlayerClass));
            val worldServerClass = ReflectionUtil.getNMSClass("WorldServer");
            MH_GET_WORLD_HANDLE = lookup.findVirtual(ReflectionUtil.getBukkitClass("CraftWorld"), "getHandle", MethodType.methodType(worldServerClass));
            MH_WORLD_DIMENSION = lookup.findGetter(worldServerClass, "dimension", Integer.TYPE);
            val minecraftServerClass = ReflectionUtil.getNMSClass("MinecraftServer");
            val craftServer = Bukkit.getServer();
            Object minecraftServer = null;
            for (val f : craftServer.getClass().getDeclaredFields()) {
                if (minecraftServerClass == f.getType()) {
                    f.setAccessible(true);
                    minecraftServer = f.get(craftPlayerClass);
                }
            }
            if (minecraftServer == null) {
                throw new RuntimeException("Cannot find MinecraftServer!");
            }
            val playerListClass = ReflectionUtil.getNMSClass("PlayerList");
            Object playerList = null;
            for (val f : minecraftServer.getClass().getSuperclass().getDeclaredFields()) {
                if (playerListClass == f.getType()) {
                    f.setAccessible(true);
                    playerList = f.get(minecraftServer);
                }
            }
            if (playerList == null) {
                throw new RuntimeException("Cannot find PlayerList!");
            }
            PLAYER_LIST = playerList;
            MH_MOVE_TO_WORLD = lookup.findVirtual(playerListClass, "moveToWorld", MethodType.methodType(entityPlayerClass, entityPlayerClass, Integer.TYPE, Boolean.TYPE, Location.class, Boolean.TYPE));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}