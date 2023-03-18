/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
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
 */
package net.skinsrestorer.bukkit.skinrefresher;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import net.skinsrestorer.bukkit.utils.BukkitReflection;
import net.skinsrestorer.mappings.shared.ViaPacketData;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public final class SpigotSkinRefresher implements Consumer<Player> {
    private final SRServerAdapter<?> adapter;
    private final Class<?> playOutRespawn;
    private final Class<?> playOutPlayerInfo;
    private final Class<?> playOutPosition;
    private final Class<?> packet;
    private final Class<?> playOutHeldItemSlot;
    private final Method getHandleMethod;
    private Enum<?> removePlayerEnum;
    private Enum<?> addPlayerEnum;
    private boolean useViabackwards = false;

    public SpigotSkinRefresher(SRServerAdapter<?> adapter, SRLogger logger) throws InitializeException {
        this.adapter = adapter;

        try {
            packet = BukkitReflection.getNMSClass("Packet", "net.minecraft.network.protocol.Packet");
            playOutHeldItemSlot = BukkitReflection.getNMSClass("PacketPlayOutHeldItemSlot", "net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot");
            playOutPosition = BukkitReflection.getNMSClass("PacketPlayOutPosition", "net.minecraft.network.protocol.game.PacketPlayOutPosition");
            playOutPlayerInfo = BukkitReflection.getNMSClass("PacketPlayOutPlayerInfo", "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo");
            playOutRespawn = BukkitReflection.getNMSClass("PacketPlayOutRespawn", "net.minecraft.network.protocol.game.PacketPlayOutRespawn");

            try {
                removePlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "EnumPlayerInfoAction", "REMOVE_PLAYER");
                addPlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
            } catch (ReflectiveOperationException e1) {
                try {
                    Class<?> enumPlayerInfoActionClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

                    // Cardboard and other platforms
                    removePlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoActionClass, 4);
                    addPlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoActionClass, 0);
                } catch (ReflectiveOperationException e2) {
                    try {
                        // Forge
                        removePlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "Action", "REMOVE_PLAYER");
                        addPlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfo, "Action", "ADD_PLAYER");
                    } catch (ReflectiveOperationException e3) {
                        try {
                            Class<?> enumPlayerInfoAction = BukkitReflection.getNMSClass("EnumPlayerInfoAction", null);

                            // 1.8
                            removePlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoAction, "REMOVE_PLAYER");
                            addPlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoAction, "ADD_PLAYER");
                        } catch (ReflectiveOperationException e4) {
                            // 1.7 and below uses a boolean instead of an enum
                        }
                    }
                }
            }

            getHandleMethod = BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("getHandle");

            adapter.runSync(() -> {
                // Wait to run task in order for ViaVersion to determine server protocol
                if (adapter.isPluginEnabled("ViaBackwards")
                        && ViaWorkaround.isProtocolNewer()) {
                    useViabackwards = true;
                    logger.debug("Activating ViaBackwards workaround.");
                }
            });

            logger.debug("Using SpigotSkinRefresher");
        } catch (Exception e) {
            throw new InitializeException(e);
        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws ReflectiveOperationException {
        ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket", new Class<?>[]{this.packet}, packet);
    }

    @Override
    public void accept(Player player) {
        try {
            final Object entityPlayer = getHandleMethod.invoke(player);
            Object removePlayer;
            Object addPlayer;
            try {
                removePlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, removePlayerEnum, ImmutableList.of(entityPlayer));
                addPlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, addPlayerEnum, ImmutableList.of(entityPlayer));
            } catch (ReflectiveOperationException e) {
                int ping = (int) ReflectionUtil.getObject(entityPlayer, "ping");
                removePlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, player.getPlayerListName(), false, 9999);
                addPlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfo, player.getPlayerListName(), true, ping);
            }

            // Slowly getting from object to object till we get what is needed for
            // the respawn packet
            Object world = ReflectionUtil.invokeMethod(entityPlayer, "getWorld");
            Object difficulty;
            try {
                difficulty = ReflectionUtil.invokeMethod(world, "getDifficulty");
            } catch (ReflectiveOperationException e) {
                difficulty = ReflectionUtil.getObject(world, "difficulty");
            }

            Object worldData;
            try {
                worldData = ReflectionUtil.invokeMethod(world, "getWorldData");
            } catch (ReflectiveOperationException ignored) {
                worldData = ReflectionUtil.getObject(world, "worldData");
            }

            Object worldType;
            try {
                worldType = ReflectionUtil.invokeMethod(worldData, "getType");
            } catch (ReflectiveOperationException ignored) {
                worldType = ReflectionUtil.invokeMethod(worldData, "getGameType");
            }

            Object playerIntManager = ReflectionUtil.getFieldByType(entityPlayer, "PlayerInteractManager");
            Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeMethod(playerIntManager, "getGameMode");

            @SuppressWarnings("deprecation")
            int gamemodeId = player.getGameMode().getValue();
            @SuppressWarnings("deprecation")
            int dimension = player.getWorld().getEnvironment().getId();

            Object respawn;
            try {
                respawn = ReflectionUtil.invokeConstructor(playOutRespawn, dimension, difficulty, worldType, enumGamemode);
            } catch (Exception ignored) {
                // 1.13.x needs the dimensionManager instead of dimension id
                Object worldObject = ReflectionUtil.getFieldByType(entityPlayer, "World");
                Object dimensionManager = getDimensionManager(worldObject, dimension);

                try {
                    respawn = ReflectionUtil.invokeConstructor(playOutRespawn, dimensionManager, difficulty, worldType, enumGamemode);
                } catch (ReflectiveOperationException ignored2) {
                    // 1.14.x removed the difficulty from PlayOutRespawn
                    // https://wiki.vg/Pre-release_protocol#Respawn
                    try {
                        respawn = ReflectionUtil.invokeConstructor(playOutRespawn, dimensionManager, worldType, enumGamemode);
                    } catch (ReflectiveOperationException ignored3) {
                        // Minecraft 1.15 changes
                        // PacketPlayOutRespawn now needs the world seed

                        long seedEncrypted = Hashing.sha256().hashString(String.valueOf(player.getWorld().getSeed()), StandardCharsets.UTF_8).asLong();
                        try {
                            respawn = ReflectionUtil.invokeConstructor(playOutRespawn, dimensionManager, seedEncrypted, worldType, enumGamemode);
                        } catch (ReflectiveOperationException ignored5) {
                            Object dimensionKey = ReflectionUtil.invokeMethod(worldObject, "getDimensionKey");
                            boolean debug = (boolean) ReflectionUtil.invokeMethod(worldObject, "isDebugWorld");
                            boolean flat = (boolean) ReflectionUtil.invokeMethod(worldObject, "isFlatWorld");
                            List<Object> gameModeList = ReflectionUtil.getFieldByTypeList(playerIntManager, "EnumGamemode");

                            Enum<?> enumGamemodePrevious = (Enum<?>) getFromListExcluded(gameModeList, enumGamemode);

                            // Minecraft 1.16.1 changes
                            try {
                                Object typeKey = ReflectionUtil.invokeMethod(worldObject, "getTypeKey");

                                respawn = ReflectionUtil.invokeConstructor(playOutRespawn, typeKey, dimensionKey, seedEncrypted, enumGamemode, enumGamemodePrevious, debug, flat, true);
                            } catch (ReflectiveOperationException ignored6) {
                                // Minecraft 1.16.2 changes
                                respawn = ReflectionUtil.invokeConstructor(playOutRespawn, dimensionManager, dimensionKey, seedEncrypted, enumGamemode, enumGamemodePrevious, debug, flat, true);
                            }
                        }
                    }
                }
            }

            Location l = player.getLocation();
            Object pos;
            try {
                // 1.17+
                pos = ReflectionUtil.invokeConstructor(playOutPosition, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0, false);
            } catch (ReflectiveOperationException e1) {
                try {
                    // 1.9-1.16.5
                    pos = ReflectionUtil.invokeConstructor(playOutPosition, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);
                } catch (ReflectiveOperationException e2) {
                    try {
                        // 1.8
                        pos = ReflectionUtil.invokeConstructor(playOutPosition, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>());
                    } catch (ReflectiveOperationException e3) {
                        // 1.7
                        pos = ReflectionUtil.invokeConstructor(playOutPosition, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), false);
                    }
                }
            }

            Object slot = ReflectionUtil.invokeConstructor(playOutHeldItemSlot, player.getInventory().getHeldItemSlot());
            Object playerCon = ReflectionUtil.getFieldByType(entityPlayer, "PlayerConnection");

            sendPacket(playerCon, removePlayer);
            sendPacket(playerCon, addPlayer);

            boolean sendRespawnPacketDirectly = true;
            if (useViabackwards) {
                try {
                    Object worldObject = ReflectionUtil.getFieldByType(entityPlayer, "World");
                    boolean flat = (boolean) ReflectionUtil.invokeMethod(worldObject, "isFlatWorld");

                    sendRespawnPacketDirectly = ViaWorkaround.sendCustomPacketVia(new ViaPacketData(player,
                            dimension,
                            Hashing.sha256().hashString(String.valueOf(player.getWorld().getSeed()), StandardCharsets.UTF_8).asLong(),
                            ((Integer) gamemodeId).shortValue(),
                            flat));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (sendRespawnPacketDirectly) {
                sendPacket(playerCon, respawn);
            }

            ReflectionUtil.invokeMethod(entityPlayer, "updateAbilities");

            sendPacket(playerCon, pos);
            sendPacket(playerCon, slot);

            ReflectionUtil.invokeMethod(player, "updateScaledHealth");
            player.updateInventory();
            ReflectionUtil.invokeMethod(entityPlayer, "triggerHealthUpdate");

            if (player.isOp()) {
                adapter.runSync(() -> {
                    // Workaround..
                    player.setOp(false);
                    player.setOp(true);
                });
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private Object getFromListExcluded(List<Object> list, Object... excluded) {
        for (Object obj : list) {
            if (obj != excluded)
                return obj;
        }

        return null;
    }

    private Object getDimensionManager(Object worldObject, int dimension) throws ReflectiveOperationException {
        try {
            return ReflectionUtil.getFieldByType(worldObject, "DimensionManager");
        } catch (ReflectiveOperationException e) {
            try {
                Class<?> dimensionManagerClass = BukkitReflection.getNMSClass("DimensionManager", "net.minecraft.world.level.dimension.DimensionManager");

                for (Method m : dimensionManagerClass.getDeclaredMethods()) {
                    if (m.getReturnType() == dimensionManagerClass && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Integer.TYPE) {
                        m.setAccessible(true);
                        return m.invoke(null, dimension);
                    }
                }
            } catch (ReflectiveOperationException e2) {
                e2.printStackTrace();
            }
        }

        throw new ReflectiveOperationException("Could not get DimensionManager from " + worldObject.getClass().getSimpleName());
    }
}
