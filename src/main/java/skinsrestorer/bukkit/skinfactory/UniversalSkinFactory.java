package skinsrestorer.bukkit.skinfactory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniversalSkinFactory extends SkinFactory {

    private Class<?> PlayOutRespawn;
    private Class<?> EntityHuman;
    private Class<?> PlayOutNamedEntitySpawn;
    private Class<?> PlayOutEntityDestroy;
    private Class<?> PlayOutPlayerInfo;
    private Class<?> PlayOutPosition;
    private Class<?> PlayOutEntityEquipment;
    private Class<?> ItemStack;
    private Class<?> Packet;
    private Class<?> CraftItemStack;
    private Class<?> PlayOutHeldItemSlot;
    private Class<?> EnumPlayerInfoAction;

    private Enum<?> PEACEFUL;
    private Enum<?> REMOVE_PLAYER;
    private Enum<?> ADD_PLAYER;
    private Enum<?> MAINHAND;
    private Enum<?> OFFHAND;
    private Enum<?> HEAD;
    private Enum<?> FEET;
    private Enum<?> LEGS;
    private Enum<?> CHEST;

    // Since literraly no one is able to optimize it, I will
    public UniversalSkinFactory() {
        try {
            Packet = ReflectionUtil.getNMSClass("Packet");
            PlayOutHeldItemSlot = ReflectionUtil.getNMSClass("PacketPlayOutHeldItemSlot");
            CraftItemStack = ReflectionUtil.getBukkitClass("inventory.CraftItemStack");
            ItemStack = ReflectionUtil.getNMSClass("ItemStack");
            PlayOutEntityEquipment = ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment");
            PlayOutPosition = ReflectionUtil.getNMSClass("PacketPlayOutPosition");
            EntityHuman = ReflectionUtil.getNMSClass("EntityHuman");
            PlayOutNamedEntitySpawn = ReflectionUtil.getNMSClass("PacketPlayOutNamedEntitySpawn");
            PlayOutEntityDestroy = ReflectionUtil.getNMSClass("PacketPlayOutEntityDestroy");
            PlayOutPlayerInfo = ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo");
            PlayOutRespawn = ReflectionUtil.getNMSClass("PacketPlayOutRespawn");
            try {
                EnumPlayerInfoAction = ReflectionUtil.getNMSClass("EnumPlayerInfoAction");
            } catch (Exception e) {
            }
            PEACEFUL = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumDifficulty"), "PEACEFUL");
            try {
                REMOVE_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "REMOVE_PLAYER");
                ADD_PLAYER = ReflectionUtil.getEnum(PlayOutPlayerInfo, "EnumPlayerInfoAction", "ADD_PLAYER");
            } catch (Exception e) {
                //1.8 or below
                REMOVE_PLAYER = ReflectionUtil.getEnum(EnumPlayerInfoAction, "REMOVE_PLAYER");
                ADD_PLAYER = ReflectionUtil.getEnum(EnumPlayerInfoAction, "ADD_PLAYER");
            }
            MAINHAND = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "MAINHAND");
            OFFHAND = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND");
            HEAD = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "HEAD");
            CHEST = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "CHEST");
            FEET = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "FEET");
            LEGS = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "LEGS");
        } catch (Exception e) {

        }
    }

    private void sendPacket(Object playerConnection, Object packet) throws Exception {
        ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket",
                new Class<?>[]{Packet}, new Object[]{packet});
    }

    @SuppressWarnings("deprecation")
    @Override
    public void updateSkin(Player player) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {

            if (!player.isOnline())
                return;

            try {
                Object ep = ReflectionUtil.invokeMethod(player, "getHandle");
                Location l = player.getLocation();

                List<Object> set = new ArrayList<>();
                set.add(ep);
                Iterable<?> iterable = set;
                Object removeInfo = null;
                Object removeEntity = null;
                Object addNamed = null;
                Object addInfo = null;

                removeInfo = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo,
                        new Class<?>[]{REMOVE_PLAYER.getClass(), Iterable.class}, REMOVE_PLAYER, iterable);
                removeEntity = ReflectionUtil.invokeConstructor(PlayOutEntityDestroy, new Class<?>[]{int[].class},
                        new int[]{player.getEntityId()});
                addNamed = ReflectionUtil.invokeConstructor(PlayOutNamedEntitySpawn, new Class<?>[]{EntityHuman},
                        ep);
                addInfo = ReflectionUtil.invokeConstructor(PlayOutPlayerInfo,
                        new Class<?>[]{ADD_PLAYER.getClass(), Iterable.class}, ADD_PLAYER, iterable);
                // Slowly getting from object to object till i get what I need for
                // the respawn packet
                Object world = ReflectionUtil.invokeMethod(ep, "getWorld");
                Object difficulty = ReflectionUtil.invokeMethod(world, "getDifficulty");
                Object worlddata = ReflectionUtil.getObject(world, "worldData");
                Object worldtype = ReflectionUtil.invokeMethod(worlddata, "getType");
                int dimension = 0;
                if (player.getWorld().getEnvironment().equals(World.Environment.NETHER))
                    dimension = -1;
                if (player.getWorld().getEnvironment().equals(World.Environment.THE_END))
                    dimension = 1;

                Object playerIntManager = ReflectionUtil.getObject(ep, "playerInteractManager");
                Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeMethod(playerIntManager, "getGameMode");

                int gmid = (int) ReflectionUtil.invokeMethod(enumGamemode, "getId");

                Object respawn;
                try {
                    respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                            new Class<?>[]{
                                    int.class, PEACEFUL.getClass(), worldtype.getClass(), enumGamemode.getClass()
                            },
                            dimension, difficulty, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gmid));
                } catch (Exception ignored) {
                    // 1.13.x needs the dimensionManager instead of dimension id
                    Class<?> dimensionManagerClass = ReflectionUtil.getNMSClass("DimensionManager");
                    Method m = dimensionManagerClass.getDeclaredMethod("a", Integer.TYPE);
                    Object dimensionManger = m.invoke(null, dimension);

                    respawn = ReflectionUtil.invokeConstructor(PlayOutRespawn,
                            new Class<?>[]{
                                    dimensionManagerClass, PEACEFUL.getClass(), worldtype.getClass(), enumGamemode.getClass()
                            },
                            dimensionManger, difficulty, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null, "getById", new Class<?>[]{int.class}, gmid));
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

                Object hand = null;
                Object mainhand = null;
                Object offhand = null;
                Object helmet = null;
                Object boots = null;
                Object chestplate = null;
                Object leggings = null;

                // MAINHAND is only null if we are less than 1.9
                if (MAINHAND == null) {

                    hand = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, int.class, ItemStack}, player.getEntityId(), 0,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class}, new Object[]{player.getItemInHand()}));

                    helmet = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, int.class, ItemStack}, player.getEntityId(), 4,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getHelmet()}));

                    chestplate = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, int.class, ItemStack}, player.getEntityId(), 3,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getChestplate()}));

                    leggings = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, int.class, ItemStack}, player.getEntityId(), 2,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getLeggings()}));

                    boots = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, int.class, ItemStack}, player.getEntityId(), 1,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class}, new Object[]{player.getInventory().getBoots()}));
                }
                // If 1.9+
                else {
                    mainhand = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, MAINHAND.getClass(), ItemStack}, player.getEntityId(), MAINHAND,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getItemInMainHand()}));

                    offhand = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, OFFHAND.getClass(), ItemStack}, player.getEntityId(), OFFHAND,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getItemInOffHand()}));

                    helmet = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, HEAD.getClass(), ItemStack}, player.getEntityId(), HEAD,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getHelmet()}));

                    chestplate = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, CHEST.getClass(), ItemStack}, player.getEntityId(), CHEST,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getChestplate()}));

                    leggings = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, LEGS.getClass(), ItemStack}, player.getEntityId(), LEGS,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class},
                                    new Object[]{player.getInventory().getLeggings()}));

                    boots = ReflectionUtil.invokeConstructor(PlayOutEntityEquipment,
                            new Class<?>[]{int.class, FEET.getClass(), ItemStack}, player.getEntityId(), FEET,
                            ReflectionUtil.invokeMethod(CraftItemStack, null, "asNMSCopy",
                                    new Class<?>[]{ItemStack.class}, new Object[]{player.getInventory().getBoots()}));
                }

                Object slot = ReflectionUtil.invokeConstructor(PlayOutHeldItemSlot, new Class<?>[]{int.class},
                        player.getInventory().getHeldItemSlot());

                for (Player pOnline : Bukkit.getOnlinePlayers()) {

                    final Object craftHandle = ReflectionUtil.invokeMethod(pOnline, "getHandle");
                    Object playerCon = ReflectionUtil.getObject(craftHandle, "playerConnection");

                    if (pOnline.equals(player)) {

                        sendPacket(playerCon, removeInfo);
                        sendPacket(playerCon, addInfo);
                        sendPacket(playerCon, respawn);

                        ReflectionUtil.invokeMethod(craftHandle, "updateAbilities");

                        sendPacket(playerCon, pos);
                        sendPacket(playerCon, slot);
                        ReflectionUtil.invokeMethod(pOnline, "updateScaledHealth");
                        ReflectionUtil.invokeMethod(pOnline, "updateInventory");
                        ReflectionUtil.invokeMethod(craftHandle, "triggerHealthUpdate");

                        if (pOnline.isOp()) {
                            pOnline.setOp(false);
                            pOnline.setOp(true);
                        }
                        continue;
                    }
                    /*Now checks if the player is in the same world and if can see the player
                     * I did that to try to prevent player duplications.
                     */
                    if (pOnline.getWorld().equals(player.getWorld()) && pOnline.canSee(player) && player.isOnline()) {
                        sendPacket(playerCon, removeEntity);
                        sendPacket(playerCon, removeInfo);
                        sendPacket(playerCon, addInfo);
                        sendPacket(playerCon, addNamed);

                        if (MAINHAND != null) {
                            sendPacket(playerCon, mainhand);
                            sendPacket(playerCon, offhand);
                        } else
                            sendPacket(playerCon, hand);

                        sendPacket(playerCon, helmet);
                        sendPacket(playerCon, chestplate);
                        sendPacket(playerCon, leggings);
                        sendPacket(playerCon, boots);
                    } else {
                        //Only sends player update packet
                        sendPacket(playerCon, removeInfo);
                        sendPacket(playerCon, addInfo);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}