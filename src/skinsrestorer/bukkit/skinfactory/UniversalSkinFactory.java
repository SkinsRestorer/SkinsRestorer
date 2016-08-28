package skinsrestorer.bukkit.skinfactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.ReflectionUtil;

public class UniversalSkinFactory implements SkinFactory {

	@Override
	public void applySkin(Player p, Object props) {
		try {
			Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(p);
			Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");
			Object profile = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
			Object propmap = ReflectionUtil.invokeMethod(profile.getClass(), profile, "getProperties");

			if (props != null) {
				ReflectionUtil.invokeMethod(propmap, "clear");
				ReflectionUtil.invokeMethod(propmap.getClass(), propmap, "put",
						new Class[] { Object.class, Object.class }, new Object[] { "textures", props });
			}
		} catch (Exception e) {
		}
	}
	@Override
	public void removeOnQuit(Player player){
		Object cp;
		try {
			cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
		Object ep = ReflectionUtil.invokeMethod(cp, "getHandle");

		List<Object> set = new ArrayList<Object>();
		set.add(ep);
		Iterable<?> iterable = set;

		Object removeInfo = ReflectionUtil
				.invokeConstructor(
						ReflectionUtil
								.getNMSClass("PacketPlayOutPlayerInfo"),
						new Class<?>[] {
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo"),
										"EnumPlayerInfoAction", "REMOVE_PLAYER").getClass(),
								Iterable.class },
				ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo"),
						"EnumPlayerInfoAction", "REMOVE_PLAYER"), iterable);

		Object removeEntity = ReflectionUtil.invokeConstructor(
				ReflectionUtil.getNMSClass("PacketPlayOutEntityDestroy"), new Class<?>[] { int[].class },
				new int[] { player.getEntityId() });
		for (Player inWorld : player.getWorld().getPlayers()) {
			Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(inWorld);
			final Object craftHandle = ReflectionUtil.invokeMethod(craftOnline, "getHandle");
			Object playerCon = ReflectionUtil.getObject(craftHandle, "playerConnection");
			sendPacket(playerCon, removeEntity);
			sendPacket(playerCon, removeInfo);
		}
		} catch (Exception e) {
		}
	}
	@SuppressWarnings("deprecation")
	@Override
	public void updateSkin(Player player) {
		if (!player.isOnline()){
			return;
		}
		try {
			Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
			Object ep = ReflectionUtil.invokeMethod(cp, "getHandle");
			Location l = player.getLocation();

			List<Object> set = new ArrayList<Object>();
			set.add(ep);
			Iterable<?> iterable = set;

			Object removeInfo = ReflectionUtil
					.invokeConstructor(
							ReflectionUtil
									.getNMSClass("PacketPlayOutPlayerInfo"),
							new Class<?>[] {
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo"),
											"EnumPlayerInfoAction", "REMOVE_PLAYER").getClass(),
									Iterable.class },
					ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo"),
							"EnumPlayerInfoAction", "REMOVE_PLAYER"), iterable);

			Object removeEntity = ReflectionUtil.invokeConstructor(
					ReflectionUtil.getNMSClass("PacketPlayOutEntityDestroy"), new Class<?>[] { int[].class },
					new int[] { player.getEntityId() });

			Object addNamed = ReflectionUtil.invokeConstructor(
					ReflectionUtil.getNMSClass("PacketPlayOutNamedEntitySpawn"),
					new Class<?>[] { ReflectionUtil.getNMSClass("EntityHuman") }, ep);

			Object addInfo = ReflectionUtil
					.invokeConstructor(
							ReflectionUtil
									.getNMSClass("PacketPlayOutPlayerInfo"),
							new Class<?>[] {
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo"),
											"EnumPlayerInfoAction", "ADD_PLAYER").getClass(),
									Iterable.class },
					ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo"),
							"EnumPlayerInfoAction", "ADD_PLAYER"), iterable);

			// Slowly getting from object to object till i get what I need for
			// the respawn packet
			Object world = ReflectionUtil.invokeMethod(ep, "getWorld");
			Object difficulty = ReflectionUtil.invokeMethod(world, "getDifficulty");
			Object worlddata = ReflectionUtil.getObject(world, "worldData");
			Object worldtype = ReflectionUtil.invokeMethod(worlddata, "getType");

			Object worldserver = ReflectionUtil.getNMSClass("WorldServer").cast(world);
			int dimension = (int) ReflectionUtil.getObject(worldserver, "dimension");

			Object playerIntManager = ReflectionUtil.getObject(ep, "playerInteractManager");
			Enum<?> enumGamemode = (Enum<?>) ReflectionUtil.invokeMethod(playerIntManager, "getGameMode");

			int gmid = (int) ReflectionUtil.invokeMethod(enumGamemode, "getId");

			Object respawn = ReflectionUtil
					.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutRespawn"),
							new Class<?>[] { int.class,
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumDifficulty"), "PEACEFUL")
											.getClass(),
									worldtype.getClass(), enumGamemode.getClass() },
							dimension, difficulty, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null,
									"getById", new Class<?>[] { int.class }, new Object[] { gmid }));

			Object pos = null;

			try {
				// 1.9+
				pos = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutPosition"),
						new Class<?>[] { double.class, double.class, double.class, float.class, float.class, Set.class,
								int.class },
						l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);
			} catch (Exception e) {
				// 1.8 -
				pos = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutPosition"),
						new Class<?>[] { double.class, double.class, double.class, float.class, float.class,
								Set.class },
						l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>());
			}

			Object hand = null;
			Object mainhand = null;
			Object offhand = null;
			Object helmet = null;
			Object boots = null;
			Object chestplate = null;
			Object leggings = null;

			Constructor<?> constr = null;

			// Check if we are using version 1.8 or below
			try {
				constr = ReflectionUtil.getConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") });

			} catch (Throwable t) {
				// If 1.9+, leave it at null
			}

			// And use packet definitons respective for these versions
			if (constr != null) {

				hand = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 0,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getItemInHand() }));

				helmet = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 4,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getInventory().getHelmet() }));

				chestplate = ReflectionUtil.invokeConstructor(
						ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 3,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getInventory().getLeggings() }));

				leggings = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 2,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getInventory().getLeggings() }));

				boots = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 1,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getInventory().getBoots() }));
			} else {
				mainhand = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class,
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "MAINHAND")
										.getClass(),
								ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(),
						ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "MAINHAND"),
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getInventory().getItemInMainHand() }));

				offhand = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class,
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND")
										.getClass(),
								ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(),
						ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND"),
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								new Object[] { player.getInventory().getItemInOffHand() }));

				helmet = ReflectionUtil
						.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
								new Class<?>[] { int.class,
										ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "HEAD")
												.getClass(),
										ReflectionUtil.getNMSClass("ItemStack") },
								player.getEntityId(),
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "HEAD"),
								ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"),
										null, "asNMSCopy", new Class<?>[] { ItemStack.class },
										new Object[] { player.getInventory().getHelmet() }));

				chestplate = ReflectionUtil
						.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
								new Class<?>[] { int.class,
										ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "CHEST")
												.getClass(),
										ReflectionUtil.getNMSClass("ItemStack") },
								player.getEntityId(),
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "CHEST"),
								ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"),
										null, "asNMSCopy", new Class<?>[] { ItemStack.class },
										new Object[] { player.getInventory().getChestplate() }));

				leggings = ReflectionUtil
						.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
								new Class<?>[] { int.class,
										ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "LEGS")
												.getClass(),
										ReflectionUtil.getNMSClass("ItemStack") },
								player.getEntityId(),
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "LEGS"),
								ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"),
										null, "asNMSCopy", new Class<?>[] { ItemStack.class },
										new Object[] { player.getInventory().getLeggings() }));

				boots = ReflectionUtil
						.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
								new Class<?>[] { int.class,
										ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "FEET")
												.getClass(),
										ReflectionUtil.getNMSClass("ItemStack") },
								player.getEntityId(),
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "FEET"),
								ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"),
										null, "asNMSCopy", new Class<?>[] { ItemStack.class },
										new Object[] { player.getInventory().getBoots() }));
			}

			Object slot = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutHeldItemSlot"),
					new Class<?>[] { int.class }, player.getInventory().getHeldItemSlot());

			// We finished defining packets, now lets send em

			for (Player inWorld : player.getWorld().getPlayers()) {
				Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(inWorld);
				final Object craftHandle = ReflectionUtil.invokeMethod(craftOnline, "getHandle");
				Object playerCon = ReflectionUtil.getObject(craftHandle, "playerConnection");
				if (inWorld.equals(player)) {
					sendPacket(playerCon, removeInfo);
					sendPacket(playerCon, addInfo);
					sendPacket(playerCon, respawn);
					Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {
						@Override
						public void run() {
							try {
								ReflectionUtil.invokeMethod(craftHandle, "updateAbilities");
							} catch (Exception e) {
							}
						}

					});
					sendPacket(playerCon, pos);
					sendPacket(playerCon, slot);
					ReflectionUtil.invokeMethod(craftOnline, "updateScaledHealth");
					ReflectionUtil.invokeMethod(craftOnline, "updateInventory");
					ReflectionUtil.invokeMethod(craftHandle, "triggerHealthUpdate");
					continue;
				}
				sendPacket(playerCon, removeEntity);
				sendPacket(playerCon, removeInfo);
				if (inWorld.canSee(player)){
				sendPacket(playerCon, addInfo);
				sendPacket(playerCon, addNamed);
				if (hand == null) {
					sendPacket(playerCon, mainhand);
					sendPacket(playerCon, offhand);
				} else {
					sendPacket(playerCon, hand);
				}
				sendPacket(playerCon, helmet);
				sendPacket(playerCon, chestplate);
				sendPacket(playerCon, leggings);
				sendPacket(playerCon, boots);
				}
			}
		} catch (Exception e) {
		}

	}

	private static void sendPacket(Object playerConnection, Object packet) throws Exception {
		ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket",
				new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, new Object[] { packet });
	}
}
