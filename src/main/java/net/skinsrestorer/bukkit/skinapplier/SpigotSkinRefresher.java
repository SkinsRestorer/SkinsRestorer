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
package net.skinsrestorer.bukkit.skinapplier;

import com.google.common.hash.Hashing;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.exception.ReflectionException;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class SpigotSkinRefresher implements Consumer<Player> {
    private final SkinsRestorer plugin;
    private Class<?> playOutRespawn;
    private Class<?> playOutPlayerInfo;
    private Class<?> playOutPosition;
    private Class<?> packet;
    private Class<?> playOutHeldItemSlot;
    private Enum<?> peaceful;
    private Enum<?> removePlayerEnum;
    private Enum<?> addPlayerEnum;
    private boolean useViabackwards = false;

    public SpigotSkinRefresher(SkinsRestorer plugin, SRLogger log) {
        this.plugin = plugin;

        try {
            packet = ReflectionUtil.getNMSClass("Packet", "net.minecraft.network.protocol.Packet");
            playOutHeldItemSlot = ReflectionUtil.getNMSClass("PacketPlayOutHeldItemSlot", "net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot");
            playOutPosition = ReflectionUtil.getNMSClass("PacketPlayOutPosition", "net.minecraft.network.protocol.game.PacketPlayOutPosition");
            playOutPlayerInfo = ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo", "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo");
            playOutRespawn = ReflectionUtil.getNMSClass("PacketPlayOutRespawn", "net.minecraft.network.protocol.game.PacketPlayOutRespawn");

            peaceful = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumDifficulty", "net.minecraft.world.EnumDifficulty"), "PEACEFUL");
            try {
                removePlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "EnumPlayerInfoAction", "REMOVE_PLAYER");
                addPlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
            } catch (Exception e) {
                try {
                    Class<?> enumPlayerInfoAction = ReflectionUtil.getNMSClass("EnumPlayerInfoAction", null);

                    // 1.8 or below
                    removePlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoAction, "REMOVE_PLAYER");
                    addPlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoAction, "ADD_PLAYER");
                } catch (Exception e1) {
                    // Forge
                    removePlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "Action", "REMOVE_PLAYER");
                    addPlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "Action", "ADD_PLAYER");
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Wait to run task in order for ViaVersion to determine server protocol
                if (plugin.getServer().getPluginManager().isPluginEnabled("ViaBackwards")
                        && ViaWorkaround.isProtocolNewer()) {
                    useViabackwards = true;
                    log.info("Activating ViaBackwards workaround.");
                }
            });

            log.info("Using SpigotSkinRefresher");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws ReflectionException {
        ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket", new Class<?>[]{this.packet}, packet);
    }

    public void accept(Player player) {
        try {
            final Object craftHandle = ReflectionUtil.invokeMethod(player, "getHandle");
            Location l = player.getLocation();

            List<Object> set = new ArrayList<>();
            set.add(craftHandle);

            Object removePlayer;
            Object addPlayer;
            try {
                //1.17+
                removePlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, new Class<?>[]{this.removePlayerEnum.getClass().getSuperclass(), Collection.class}, this.removePlayerEnum, set);
                addPlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, new Class<?>[]{this.addPlayerEnum.getClass().getSuperclass(), Collection.class}, this.addPlayerEnum, set);
            } catch (Exception ignored) {
                removePlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, new Class<?>[]{this.removePlayerEnum.getClass(), Iterable.class}, this.removePlayerEnum, set);
                addPlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, new Class<?>[]{this.addPlayerEnum.getClass(), Iterable.class}, this.addPlayerEnum, set);
            }

            // Slowly getting from object to object till i get what I need for
            // the respawn packet
            Object world = ReflectionUtil.invokeMethod(craftHandle, "getWorld");
            Object difficulty = ReflectionUtil.invokeMethod(world, "getDifficulty");

            Object worldData;
            try {
                worldData = ReflectionUtil.invokeMethod(world, "getWorldData");
            } catch (Exception ignored) {
                worldData = ReflectionUtil.getObject(world, "worldData");
            }

            Object worldType;
            try {
                worldType = ReflectionUtil.invokeMethod(worldData, "getType");
            } catch (Exception ignored) {
                worldType = ReflectionUtil.invokeMethod(worldData, "getGameType");
            }

            World.Environment environment = player.getWorld().getEnvironment();

            int dimension = 0;

            Object playerIntManager = ReflectionUtil.getFieldByClassName(craftHandle, "PlayerInteractManager");


            Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeMethod(playerIntManager, "getGameMode");

            int gamemodeId = player.getGameMode().getValue();

            Object respawn;
            try {
                dimension = environment.getId();
                respawn = ReflectionUtil.invokeConstructor(playOutRespawn,
                        new Class<?>[]{
                                int.class, peaceful.getClass(), worldType.getClass(), enumGamemode.getClass()
                        },
                        dimension, difficulty, worldType, enumGamemode);
            } catch (Exception ignored) {
                if (environment.equals(World.Environment.NETHER))
                    dimension = -1;
                else if (environment.equals(World.Environment.THE_END))
                    dimension = 1;

                // 1.13.x needs the dimensionManager instead of dimension id
                Class<?> dimensionManagerClass = ReflectionUtil.getNMSClass("DimensionManager", "net.minecraft.world.level.dimension.DimensionManager");
                Method m = dimensionManagerClass.getDeclaredMethod("a", Integer.TYPE);

                Object dimensionManger = null;
                try {
                    dimensionManger = m.invoke(null, dimension);
                } catch (Exception ignored2) {
                }

                try {
                    respawn = ReflectionUtil.invokeConstructor(playOutRespawn,
                            new Class<?>[]{
                                    dimensionManagerClass, peaceful.getClass(), worldType.getClass(), enumGamemode.getClass()
                            },
                            dimensionManger, difficulty, worldType, enumGamemode);
                } catch (Exception ignored2) {
                    // 1.14.x removed the difficulty from PlayOutRespawn
                    // https://wiki.vg/Pre-release_protocol#Respawn
                    try {
                        respawn = ReflectionUtil.invokeConstructor(playOutRespawn,
                                new Class<?>[]{
                                        dimensionManagerClass, worldType.getClass(), enumGamemode.getClass()
                                },
                                dimensionManger, worldType, enumGamemode);
                    } catch (Exception ignored3) {
                        // Minecraft 1.15 changes
                        // PacketPlayOutRespawn now needs the world seed

                        Object seed;
                        try {
                            seed = ReflectionUtil.invokeMethod(worldData, "getSeed");
                        } catch (Exception ignored4) {
                            // 1.16
                            seed = ReflectionUtil.invokeMethod(world, "getSeed");
                        }

                        //noinspection UnstableApiUsage
                        long seedEncrypted = Hashing.sha256().hashString(seed.toString(), StandardCharsets.UTF_8).asLong();
                        try {
                            respawn = ReflectionUtil.invokeConstructor(playOutRespawn,
                                    new Class<?>[]{
                                            dimensionManagerClass, long.class, worldType.getClass(), enumGamemode.getClass()
                                    },
                                    dimensionManger, seedEncrypted, worldType, enumGamemode);
                        } catch (Exception ignored5) {
                            // Minecraft 1.16.1 changes
                            try {
                                Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");

                                Object typeKey = ReflectionUtil.invokeMethod(worldServer, "getTypeKey");
                                Object dimensionKey = ReflectionUtil.invokeMethod(worldServer, "getDimensionKey");

                                respawn = ReflectionUtil.invokeConstructor(playOutRespawn,
                                        new Class<?>[]{
                                                typeKey.getClass(), dimensionKey.getClass(), long.class, enumGamemode.getClass(), enumGamemode.getClass(), boolean.class, boolean.class, boolean.class
                                        },
                                        typeKey,
                                        dimensionKey,
                                        seedEncrypted,
                                        enumGamemode,
                                        enumGamemode,
                                        ReflectionUtil.invokeMethod(worldServer, "isDebugWorld"),
                                        ReflectionUtil.invokeMethod(worldServer, "isFlatWorld"),
                                        true
                                );
                            } catch (Exception ignored6) {
                                // Minecraft 1.16.2 changes
                                Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");

                                Object dimensionManager = ReflectionUtil.invokeMethod(worldServer, "getDimensionManager");
                                Object dimensionKey = ReflectionUtil.invokeMethod(worldServer, "getDimensionKey");

                                respawn = ReflectionUtil.invokeConstructor(playOutRespawn,
                                        new Class<?>[]{
                                                dimensionManager.getClass(), dimensionKey.getClass(), long.class, enumGamemode.getClass(), enumGamemode.getClass(), boolean.class, boolean.class, boolean.class
                                        },
                                        dimensionManager,
                                        dimensionKey,
                                        seedEncrypted,
                                        enumGamemode,
                                        enumGamemode,
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
                // 1.17+
                pos = ReflectionUtil.invokeConstructor(playOutPosition,
                        new Class<?>[]{double.class, double.class, double.class, float.class, float.class, Set.class,
                                int.class, boolean.class},
                        l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0, false);
            } catch (Exception e1) {
                try {
                    // 1.9-1.16.5
                    pos = ReflectionUtil.invokeConstructor(playOutPosition,
                            new Class<?>[]{double.class, double.class, double.class, float.class, float.class, Set.class,
                                    int.class},
                            l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);
                } catch (Exception e2) {
                    // 1.8 -
                    pos = ReflectionUtil.invokeConstructor(playOutPosition,
                            new Class<?>[]{double.class, double.class, double.class, float.class, float.class,
                                    Set.class},
                            l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>());
                }
            }


            Object slot = ReflectionUtil.invokeConstructor(playOutHeldItemSlot, new Class<?>[]{int.class}, player.getInventory().getHeldItemSlot());

            Object playerCon = ReflectionUtil.getFieldByClassName(craftHandle, "PlayerConnection");

            sendPacket(playerCon, removePlayer);
            sendPacket(playerCon, addPlayer);

            boolean sendRespawnPacketDirectly = true;
            if (useViabackwards) {
                try {
                    sendRespawnPacketDirectly = ViaWorkaround.sendCustomPacketVia(player, craftHandle, dimension, world, gamemodeId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (sendRespawnPacketDirectly) {
                sendPacket(playerCon, respawn);
            }

            ReflectionUtil.invokeMethod(craftHandle, "updateAbilities");

            sendPacket(playerCon, pos);
            sendPacket(playerCon, slot);

            ReflectionUtil.invokeMethod(player, "updateScaledHealth");
            player.updateInventory();
            ReflectionUtil.invokeMethod(craftHandle, "triggerHealthUpdate");

            if (player.isOp()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Workaround..
                    player.setOp(false);
                    player.setOp(true);
                });
            }
        } catch (ReflectionException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
