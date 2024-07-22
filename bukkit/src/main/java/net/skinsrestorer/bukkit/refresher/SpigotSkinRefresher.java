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
package net.skinsrestorer.bukkit.refresher;

import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.bukkit.mappings.ViaPacketData;
import net.skinsrestorer.bukkit.utils.BukkitReflection;
import net.skinsrestorer.bukkit.utils.HandleReflection;
import net.skinsrestorer.bukkit.utils.OPRefreshUtil;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;

public final class SpigotSkinRefresher implements SkinRefresher {
    private final SRBukkitAdapter adapter;
    private final SRLogger logger;
    private final ViaRefreshProvider viaProvider;
    private final Class<?> playOutRespawnClass;
    private final Class<?> playOutPlayerInfoClass;
    private final Class<?> playOutPositionClass;
    private final Class<?> packetClass;
    private final Class<?> playOutHeldItemSlotClass;
    private final Enum<?> removePlayerEnum;
    private final Enum<?> addPlayerEnum;

    @Inject
    public SpigotSkinRefresher(SRBukkitAdapter adapter, SRLogger logger, ViaRefreshProvider viaProvider) {
        this.adapter = adapter;
        this.logger = logger;
        this.viaProvider = viaProvider;

        try {
            this.packetClass = BukkitReflection.getNMSClass("Packet", "net.minecraft.network.protocol.Packet");
            this.playOutHeldItemSlotClass = BukkitReflection.getNMSClass("PacketPlayOutHeldItemSlot", "net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot");
            this.playOutPositionClass = BukkitReflection.getNMSClass("PacketPlayOutPosition", "net.minecraft.network.protocol.game.PacketPlayOutPosition");
            this.playOutPlayerInfoClass = BukkitReflection.getNMSClass("PacketPlayOutPlayerInfo", "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo");
            this.playOutRespawnClass = BukkitReflection.getNMSClass("PacketPlayOutRespawn", "net.minecraft.network.protocol.game.PacketPlayOutRespawn");

            Enum<?> removePlayerEnum;
            Enum<?> addPlayerEnum;
            try {
                removePlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfoClass, "EnumPlayerInfoAction", "REMOVE_PLAYER");
                addPlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfoClass, "EnumPlayerInfoAction", "ADD_PLAYER");
            } catch (ReflectiveOperationException e1) {
                try {
                    Class<?> enumPlayerInfoActionClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

                    // Cardboard and other platforms
                    removePlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoActionClass, 4);
                    addPlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoActionClass, 0);
                } catch (ReflectiveOperationException e2) {
                    try {
                        // Forge
                        removePlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfoClass, "Action", "REMOVE_PLAYER");
                        addPlayerEnum = ReflectionUtil.getEnum(playOutPlayerInfoClass, "Action", "ADD_PLAYER");
                    } catch (ReflectiveOperationException e3) {
                        try {
                            Class<?> enumPlayerInfoAction = BukkitReflection.getNMSClass("EnumPlayerInfoAction", null);

                            // 1.8
                            removePlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoAction, "REMOVE_PLAYER");
                            addPlayerEnum = ReflectionUtil.getEnum(enumPlayerInfoAction, "ADD_PLAYER");
                        } catch (ReflectiveOperationException e4) {
                            // 1.7 and below uses a boolean instead of an enum
                            removePlayerEnum = null;
                            addPlayerEnum = null;
                        }
                    }
                }
            }

            this.removePlayerEnum = removePlayerEnum;
            this.addPlayerEnum = addPlayerEnum;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize SpigotSkinRefresher", e);
        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws ReflectiveOperationException {
        ReflectionUtil.invokeObjectMethod(
                playerConnection,
                "sendPacket",
                new ReflectionUtil.ParameterPair<>(packetClass, packet)
        );
    }

    @Override
    public void refresh(Player player) {
        try {
            final Object entityPlayer = HandleReflection.getHandle(player, Object.class);
            Object removePlayer;
            Object addPlayer;
            try {
                removePlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfoClass, removePlayerEnum, List.of(entityPlayer));
                addPlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfoClass, addPlayerEnum, List.of(entityPlayer));
            } catch (ReflectiveOperationException e) {
                try {
                    int ping = ReflectionUtil.getObject(entityPlayer, "ping");
                    removePlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfoClass, player.getPlayerListName(), false, 9999);
                    addPlayer = ReflectionUtil.invokeConstructor(playOutPlayerInfoClass, player.getPlayerListName(), true, ping);
                } catch (ReflectiveOperationException e2) {
                    // 1.7.10 and below | pre-netty
                    removePlayer = ReflectionUtil.invokeStaticMethod(playOutPlayerInfoClass, "removePlayer", new ReflectionUtil.ParameterPair<>(entityPlayer));
                    addPlayer = ReflectionUtil.invokeStaticMethod(playOutPlayerInfoClass, "addPlayer", new ReflectionUtil.ParameterPair<>(entityPlayer));
                }
            }

            // Slowly getting from object to object till we get what is needed for
            // the respawn packet
            Object world = ReflectionUtil.invokeObjectMethod(entityPlayer, "getWorld");
            Object difficulty;
            try {
                difficulty = ReflectionUtil.invokeObjectMethod(world, "getDifficulty");
            } catch (ReflectiveOperationException e) {
                difficulty = ReflectionUtil.getObject(world, "difficulty");
            }

            Object worldData;
            try {
                worldData = ReflectionUtil.invokeObjectMethod(world, "getWorldData");
            } catch (ReflectiveOperationException ignored) {
                worldData = ReflectionUtil.getObject(world, "worldData");
            }

            Object worldType;
            try {
                worldType = ReflectionUtil.invokeObjectMethod(worldData, "getType");
            } catch (ReflectiveOperationException ignored) {
                worldType = ReflectionUtil.invokeObjectMethod(worldData, "getGameType");
            }

            Object playerIntManager = ReflectionUtil.getFieldByType(entityPlayer, "PlayerInteractManager");
            Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeObjectMethod(playerIntManager, "getGameMode");

            @SuppressWarnings("deprecation")
            int gamemodeId = player.getGameMode().getValue();
            @SuppressWarnings("deprecation")
            int dimension = player.getWorld().getEnvironment().getId();

            Object respawn;
            try {
                respawn = ReflectionUtil.invokeConstructor(playOutRespawnClass, dimension, difficulty, worldType, enumGamemode);
            } catch (Exception ignored) {
                // 1.13.x needs the dimensionManager instead of dimension id
                Object worldObject = ReflectionUtil.getFieldByType(entityPlayer, "World");
                Object dimensionManager = getDimensionManager(worldObject, dimension);

                try {
                    respawn = ReflectionUtil.invokeConstructor(playOutRespawnClass, dimensionManager, difficulty, worldType, enumGamemode);
                } catch (ReflectiveOperationException ignored2) {
                    // 1.14.x removed the difficulty from PlayOutRespawn
                    // https://wiki.vg/Pre-release_protocol#Respawn
                    try {
                        respawn = ReflectionUtil.invokeConstructor(playOutRespawnClass, dimensionManager, worldType, enumGamemode);
                    } catch (ReflectiveOperationException ignored3) {
                        // Minecraft 1.15 changes
                        // PacketPlayOutRespawn now needs the world seed

                        long seedEncrypted = SRHelpers.hashSha256ToLong(String.valueOf(player.getWorld().getSeed()));
                        try {
                            respawn = ReflectionUtil.invokeConstructor(playOutRespawnClass, dimensionManager, seedEncrypted, worldType, enumGamemode);
                        } catch (ReflectiveOperationException ignored5) {
                            Object dimensionKey = ReflectionUtil.invokeObjectMethod(worldObject, "getDimensionKey");
                            boolean debug = (boolean) ReflectionUtil.invokeObjectMethod(worldObject, "isDebugWorld");
                            boolean flat = (boolean) ReflectionUtil.invokeObjectMethod(worldObject, "isFlatWorld");
                            List<Object> gameModeList = ReflectionUtil.getFieldByTypeList(playerIntManager, "EnumGamemode");

                            Enum<?> enumGamemodePrevious = (Enum<?>) getFromListExcluded(gameModeList, enumGamemode);

                            // Minecraft 1.16.1 changes
                            try {
                                Object typeKey = ReflectionUtil.invokeObjectMethod(worldObject, "getTypeKey");

                                respawn = ReflectionUtil.invokeConstructor(playOutRespawnClass, typeKey, dimensionKey, seedEncrypted, enumGamemode, enumGamemodePrevious, debug, flat, true);
                            } catch (ReflectiveOperationException ignored6) {
                                // Minecraft 1.16.2 changes
                                respawn = ReflectionUtil.invokeConstructor(playOutRespawnClass, dimensionManager, dimensionKey, seedEncrypted, enumGamemode, enumGamemodePrevious, debug, flat, true);
                            }
                        }
                    }
                }
            }

            Location l = player.getLocation();
            Object pos;
            try {
                // 1.17+
                pos = ReflectionUtil.invokeConstructor(playOutPositionClass, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0, false);
            } catch (ReflectiveOperationException e1) {
                try {
                    // 1.9-1.16.5
                    pos = ReflectionUtil.invokeConstructor(playOutPositionClass, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);
                } catch (ReflectiveOperationException e2) {
                    try {
                        // 1.8
                        pos = ReflectionUtil.invokeConstructor(playOutPositionClass, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>());
                    } catch (ReflectiveOperationException e3) {
                        // 1.7
                        pos = ReflectionUtil.invokeConstructor(playOutPositionClass, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), false);
                    }
                }
            }

            Object slot = ReflectionUtil.invokeConstructor(playOutHeldItemSlotClass, player.getInventory().getHeldItemSlot());
            Object playerCon = ReflectionUtil.getFieldByType(entityPlayer, "PlayerConnection");

            sendPacket(playerCon, removePlayer);
            sendPacket(playerCon, addPlayer);

            boolean sendRespawnPacketDirectly = viaProvider.test(() -> {
                Object worldObject = ReflectionUtil.getFieldByType(entityPlayer, "World");
                boolean flat = (boolean) ReflectionUtil.invokeObjectMethod(worldObject, "isFlatWorld");

                return new ViaPacketData(
                        player,
                        SRHelpers.hashSha256ToLong(String.valueOf(player.getWorld().getSeed())),
                        ((Integer) gamemodeId).shortValue(),
                        flat
                );
            });

            if (sendRespawnPacketDirectly) {
                sendPacket(playerCon, respawn);
            }

            ReflectionUtil.invokeObjectMethod(entityPlayer, "updateAbilities");

            sendPacket(playerCon, pos);
            sendPacket(playerCon, slot);

            ReflectionUtil.invokeObjectMethod(player, "updateScaledHealth");
            player.updateInventory();
            ReflectionUtil.invokeObjectMethod(entityPlayer, "triggerHealthUpdate");

            // TODO: Resend potion effects

            // TODO: Send proper permission level instead of this workaround
            OPRefreshUtil.refreshOP(player, adapter);
        } catch (ReflectiveOperationException e) {
            logger.severe("Failed to refresh skin for player %s".formatted(player.getName()), e);
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
                logger.severe("Failed to get DimensionManager from %s".formatted(worldObject.getClass().getSimpleName()), e2);
            }
        }

        throw new ReflectiveOperationException("Could not get DimensionManager from %s".formatted(worldObject.getClass().getSimpleName()));
    }
}
