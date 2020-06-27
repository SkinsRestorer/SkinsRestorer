package skinsrestorer.bukkit.skinfactory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class OldSkinRefresher implements Consumer<Player> {

    private static Class<?> PlayOutRespawn;
    private static Class<?> PlayOutPlayerInfo;
    private static Class<?> PlayOutPosition;
    private static Class<?> Packet;
    private static Class<?> PlayOutHeldItemSlot;
    private static Class<?> EnumPlayerInfoAction;

    private static Enum<?> PEACEFUL;
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
                EnumPlayerInfoAction = ReflectionUtil.getNMSClass("EnumPlayerInfoAction");
            } catch (Exception ignored) {
            }
            PEACEFUL = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumDifficulty"), "PEACEFUL");
            try {
                REMOVE_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "REMOVE_PLAYER");
                ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
            } catch (Exception e) {
                try {
                    //1.8 or below
                    REMOVE_PLAYER = ReflectionUtil.getEnum(EnumPlayerInfoAction, "REMOVE_PLAYER");
                    ADD_PLAYER = ReflectionUtil.getEnum(EnumPlayerInfoAction, "ADD_PLAYER");
                } catch (Exception e1) {
                    // Forge
                    REMOVE_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "Action", "REMOVE_PLAYER");
                    ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "Action", "ADD_PLAYER");
                }
            }
            System.out.println("[SkinsRestorer] Using SpigotSkinRefresher");
        } catch (Exception ignored) {
        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws Exception {
        ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket", new Class<?>[]{Packet}, packet);
    }

    public void accept(Player player) {
        try {
            Object ep = ReflectionUtil.invokeMethod(player, "getHandle");
            Location l = player.getLocation();

            List<Object> set = new ArrayList<>();
            set.add(ep);

            Object removePlayer;
            Object addPlayer;

            removePlayer = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo, new Class<?>[]{REMOVE_PLAYER.getClass(), Iterable.class}, REMOVE_PLAYER, set);
            addPlayer = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo, new Class<?>[]{ADD_PLAYER.getClass(), Iterable.class}, ADD_PLAYER, set);

            // Slowly getting from object to object till i get what I need for
            // the respawn packet
            Object world = ReflectionUtil.invokeMethod(ep, "getWorld");
            Object difficulty = ReflectionUtil.invokeMethod(world, "getDifficulty");
            Object worlddata = ReflectionUtil.getObject(world, "worldData");

            Object worldtype;

            try {
                worldtype = ReflectionUtil.invokeMethod(worlddata, "getType");
            } catch (Exception ignored) {
                worldtype = ReflectionUtil.invokeMethod(worlddata, "getGameType");
            }

            World.Environment environment = player.getWorld().getEnvironment();

            int dimension = 0;

            Object playerIntManager = ReflectionUtil.getObject(ep, "playerInteractManager");
            Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeMethod(playerIntManager, "getGameMode");

            int gamemodeId = (int) ReflectionUtil.invokeMethod(enumGamemode, "getId");

            Object respawn;
            try {
                dimension = environment.getId();
                respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                        new Class<?>[]{
                                int.class, PEACEFUL.getClass(), worldtype.getClass(), enumGamemode.getClass()
                        },
                        dimension, difficulty, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId));
            } catch (Exception ignored) {
                if (environment.equals(World.Environment.NETHER))
                    dimension = -1;
                else if (environment.equals(World.Environment.THE_END))
                    dimension = 1;

                // 1.13.x needs the dimensionManager instead of dimension id
                Class<?> dimensionManagerClass = ReflectionUtil.getNMSClass("DimensionManager");
                Method m = dimensionManagerClass.getDeclaredMethod("a", Integer.TYPE);

                // Todo: Broken on 1.16.1
                Object dimensionManger = null;
                try {
                    dimensionManger = m.invoke(null, dimension);
                } catch (Exception ignored2) {
                }

                try {
                    respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                            new Class<?>[]{
                                    dimensionManagerClass, PEACEFUL.getClass(), worldtype.getClass(), enumGamemode.getClass()
                            },
                            dimensionManger, difficulty, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId));
                } catch (Exception ignored2) {
                    // 1.14.x removed the difficulty from PlayOutRespawn
                    // https://wiki.vg/Pre-release_protocol#Respawn
                    try {
                        respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                                new Class<?>[]{
                                        dimensionManagerClass, worldtype.getClass(), enumGamemode.getClass()
                                },
                                dimensionManger, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId));
                    } catch (Exception ignored3) {
                        // Minecraft 1.15 changes
                        // PacketPlayOutRespawn now needs the world seed

                        Object seed;
                        try {
                            seed = ReflectionUtil.invokeMethod(worlddata, "getSeed");
                        } catch (Exception ignored4) {
                            // 1.16.1
                            seed = ReflectionUtil.invokeMethod(world, "getSeed");
                        }

                        try {
                            respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                                    new Class<?>[]{
                                            dimensionManagerClass, long.class, worldtype.getClass(), enumGamemode.getClass()
                                    },
                                    dimensionManger, seed, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId));
                        } catch (Exception ignored5) {
                            // Minecraft 1.16.1 changes

                            Class<?> resourceKeyClass = ReflectionUtil.getNMSClass("ResourceKey");
                            Class<?> minecraftKeyClass = ReflectionUtil.getNMSClass("MinecraftKey");

                            Method rkM = resourceKeyClass.getDeclaredMethod("a", minecraftKeyClass, minecraftKeyClass);
                            Method mkM = minecraftKeyClass.getDeclaredMethod("a", String.class);
                            rkM.setAccessible(true);
                            mkM.setAccessible(true);

                            Object worldNameKey = mkM.invoke(null, "name");

                            Object worldNameValue;
                            if (dimension == 0)
                                worldNameValue = mkM.invoke(null, "minecraft:overworld");
                            else if (dimension == -1)
                                worldNameValue = mkM.invoke(null, "minecraft:the_nether");
                            else
                                worldNameValue = mkM.invoke(null, "minecraft:the_end");

                            Object worldIdentifier = rkM.invoke(null, worldNameKey, worldNameValue);

                            respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                                    new Class<?>[]{
                                            resourceKeyClass, resourceKeyClass, long.class, enumGamemode.getClass(), enumGamemode.getClass(), boolean.class, boolean.class, boolean.class
                                    },
                                    worldIdentifier,  // Todo: Proper Dimension stuff
                                    worldIdentifier,
                                    seed,
                                    ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId),
                                    ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId),
                                    false,
                                    false,
                                    false
                            );
                        }
                    }
                }
            }

            Object pos;

            try {
                // 1.9+
                pos = ReflectionUtil.invokeConstructor(PlayOutPosition,
                        new Class<?>[]{double.class, double.class, double.class, float.class, float.class, Set.class,
                                int.class},
                        l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);
            } catch (Exception e) {
                // 1.8 -
                pos = ReflectionUtil.invokeConstructor(PlayOutPosition,
                        new Class<?>[]{double.class, double.class, double.class, float.class, float.class,
                                Set.class},
                        l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>());
            }

            Object slot = ReflectionUtil.invokeConstructor(PlayOutHeldItemSlot, new Class<?>[]{int.class}, player.getInventory().getHeldItemSlot());

            final Object craftHandle = ReflectionUtil.invokeMethod(player, "getHandle");
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
    }
}