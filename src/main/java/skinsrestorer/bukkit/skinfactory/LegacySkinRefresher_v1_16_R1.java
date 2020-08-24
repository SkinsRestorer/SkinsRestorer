package skinsrestorer.bukkit.skinfactory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by McLive on 26.06.2020.
 */
public class LegacySkinRefresher_v1_16_R1 implements Consumer<Player> {

    private static Class<?> PlayOutRespawn;
    private static Class<?> PlayOutPlayerInfo;
    private static Class<?> PlayOutPosition;
    private static Class<?> Packet;
    private static Class<?> PlayOutHeldItemSlot;

    private static Enum<?> REMOVE_PLAYER;
    private static Enum<?> ADD_PLAYER;

    static {
        try {
            Packet = ReflectionUtil.getNMSClass("Packet");
            PlayOutHeldItemSlot = ReflectionUtil.getNMSClass("PacketPlayOutHeldItemSlot");
            PlayOutPosition = ReflectionUtil.getNMSClass("PacketPlayOutPosition");
            PlayOutPlayerInfo = ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo");
            PlayOutRespawn = ReflectionUtil.getNMSClass("PacketPlayOutRespawn");
            try {
                REMOVE_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "REMOVE_PLAYER");
                ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
            } catch (Exception e) {
                // Forge
                REMOVE_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "Action", "REMOVE_PLAYER");
                ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "Action", "ADD_PLAYER");
            }

            System.out.println("[SkinsRestorer] Using LegacySkinRefresher_v1_16_R1");
        } catch (Exception ignored) {
        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws Exception {
        ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket", new Class<?>[]{Packet}, packet);
    }

    public void accept(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            try {
                final Object craftHandle = ReflectionUtil.invokeMethod(player, "getHandle");
                Location l = player.getLocation();

                List<Object> set = new ArrayList<>();
                set.add(craftHandle);

                Object removePlayer = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo, new Class<?>[]{REMOVE_PLAYER.getClass(), Iterable.class}, REMOVE_PLAYER, set);
                Object addPlayer = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo, new Class<?>[]{ADD_PLAYER.getClass(), Iterable.class}, ADD_PLAYER, set);

                Object world = ReflectionUtil.invokeMethod(craftHandle, "getWorld");

                Object playerIntManager = ReflectionUtil.getObject(craftHandle, "playerInteractManager");
                Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeMethod(playerIntManager, "getGameMode");
                int gameModeId = (int) ReflectionUtil.invokeMethod(enumGamemode, "getId");

                Object seed = ReflectionUtil.invokeMethod(world, "getSeed");

                Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");
                Object typeKey = ReflectionUtil.invokeMethod(worldServer, "getTypeKey");
                Object dimensionKey = ReflectionUtil.invokeMethod(worldServer, "getDimensionKey");

                Object respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                        new Class<?>[]{
                                typeKey.getClass(), dimensionKey.getClass(), long.class, enumGamemode.getClass(), enumGamemode.getClass(), boolean.class, boolean.class, boolean.class
                        },
                        typeKey,
                        dimensionKey,
                        seed,
                        ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gameModeId),
                        ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gameModeId),
                        ReflectionUtil.invokeMethod(worldServer, "isDebugWorld"),
                        ReflectionUtil.invokeMethod(worldServer, "isFlatWorld"),
                        true
                );

                Object pos = ReflectionUtil.invokeConstructor(PlayOutPosition,
                        new Class<?>[]{double.class, double.class, double.class, float.class, float.class, Set.class,
                                int.class},
                        l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);

                Object slot = ReflectionUtil.invokeConstructor(PlayOutHeldItemSlot, new Class<?>[]{int.class}, player.getInventory().getHeldItemSlot());

                Object playerCon = ReflectionUtil.getObject(craftHandle, "playerConnection");

                sendPacket(playerCon, removePlayer);
                sendPacket(playerCon, addPlayer);

                sendPacket(playerCon, respawn);

                ReflectionUtil.invokeMethod(craftHandle, "updateAbilities");

                sendPacket(playerCon, pos);
                sendPacket(playerCon, slot);

                ReflectionUtil.invokeMethod(player, "updateScaledHealth");
                ReflectionUtil.invokeMethod(player, "updateInventory");
                ReflectionUtil.invokeMethod(craftHandle, "triggerHealthUpdate");

                if (player.isOp()) {
                    Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), () -> {
                        // Workaround..
                        player.setOp(false);
                        player.setOp(true);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
