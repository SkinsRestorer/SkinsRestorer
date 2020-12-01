package skinsrestorer.bukkit.skinfactory;

import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.ReflectionUtil;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

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

    private static boolean USE_VIABACKWARDS = false;

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

            SkinsRestorer.getInstance().getServer().getScheduler().runTask(SkinsRestorer.getInstance(), () -> {
                // Wait to run task in order for ViaVersion to determine server protocol
                if (SkinsRestorer.getInstance().getServer().getPluginManager().isPluginEnabled("ViaBackwards")) {
                    if (ProtocolRegistry.SERVER_PROTOCOL >= ProtocolVersion.v1_16.getVersion()) {
                        USE_VIABACKWARDS = true;
                        SkinsRestorer.getInstance().getLogger().log(Level.INFO, "Activating ViaBackwards workaround.");
                    }
                }
            });

            System.out.println("[SkinsRestorer] Using SpigotSkinRefresher");
        } catch (Exception ignored) {
        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws Exception {
        ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket", new Class<?>[]{Packet}, packet);
    }

    public void accept(Player player) {
        try {
            final Object craftHandle = ReflectionUtil.invokeMethod(player, "getHandle");
            Location l = player.getLocation();

            List<Object> set = new ArrayList<>();
            set.add(craftHandle);

            Object removePlayer;
            Object addPlayer;

            removePlayer = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo, new Class<?>[]{REMOVE_PLAYER.getClass(), Iterable.class}, REMOVE_PLAYER, set);
            addPlayer = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo, new Class<?>[]{ADD_PLAYER.getClass(), Iterable.class}, ADD_PLAYER, set);

            // Slowly getting from object to object till i get what I need for
            // the respawn packet
            Object world = ReflectionUtil.invokeMethod(craftHandle, "getWorld");
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

            Object playerIntManager = ReflectionUtil.getObject(craftHandle, "playerInteractManager");
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
                            try {
                                Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");

                                Object typeKey = ReflectionUtil.invokeMethod(worldServer, "getTypeKey");
                                Object dimensionKey = ReflectionUtil.invokeMethod(worldServer, "getDimensionKey");

                                respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                                        new Class<?>[]{
                                                typeKey.getClass(), dimensionKey.getClass(), long.class, enumGamemode.getClass(), enumGamemode.getClass(), boolean.class, boolean.class, boolean.class
                                        },
                                        typeKey,
                                        dimensionKey,
                                        seed,
                                        ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId),
                                        ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId),
                                        ReflectionUtil.invokeMethod(worldServer, "isDebugWorld"),
                                        ReflectionUtil.invokeMethod(worldServer, "isFlatWorld"),
                                        true
                                );
                            } catch (Exception ignored6) {
                                // Minecraft 1.16.2 changes

                                Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");

                                Object DimensionManager = ReflectionUtil.invokeMethod(worldServer, "getDimensionManager");
                                Object dimensionKey = ReflectionUtil.invokeMethod(worldServer, "getDimensionKey");

                                respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                                        new Class<?>[]{
                                                DimensionManager.getClass(), dimensionKey.getClass(), long.class, enumGamemode.getClass(), enumGamemode.getClass(), boolean.class, boolean.class, boolean.class
                                        },
                                        DimensionManager,
                                        dimensionKey,
                                        seed,
                                        ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId),
                                        ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gamemodeId),
                                        ReflectionUtil.invokeMethod(worldServer, "isDebugWorld"),
                                        ReflectionUtil.invokeMethod(worldServer, "isFlatWorld"),
                                        true
                                );
                            }
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

            Object playerCon = ReflectionUtil.getObject(craftHandle, "playerConnection");

            sendPacket(playerCon, removePlayer);
            sendPacket(playerCon, addPlayer);

            boolean sendRespawnPacketDirectly = true;
            if (USE_VIABACKWARDS) {
                UserConnection connection = Via.getManager().getConnection(player.getUniqueId());
                if (connection != null) {
                    if (connection.getProtocolInfo() != null && connection.getProtocolInfo().getProtocolVersion() < ProtocolVersion.v1_16.getVersion()) {
                        // ViaBackwards double-sends a respawn packet when its dimension ID matches the current world's.
                        // In order to get around this, we send a packet directly into Via's connection, bypassing the 1.16 conversion step
                        // and therefore bypassing their workaround.
                        // TODO: This assumes 1.16 methods; probably stop hardcoding this when 1.17 comes around
                        Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");

                        PacketWrapper packet = new PacketWrapper(ClientboundPackets1_15.RESPAWN.ordinal(), null, connection);
                        packet.write(Type.INT, dimension);
                        packet.write(Type.LONG, (long) ReflectionUtil.invokeMethod(world, "getSeed"));
                        packet.write(Type.UNSIGNED_BYTE, (short) gamemodeId);
                        packet.write(Type.STRING, (boolean) ReflectionUtil.invokeMethod(worldServer, "isFlatWorld") ? "flat" : "default");
                        packet.send(Protocol1_15_2To1_16.class, true, true);
                        sendRespawnPacketDirectly = false;
                    }
                }
            }

            if (sendRespawnPacketDirectly) {
                sendPacket(playerCon, respawn);
            }

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
