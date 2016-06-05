package skinsrestorer.bukkit;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

public class UniversalSkinFactory {

	/** Class by Blackfire62 **/

	public void applySkin(final Player player) {
		final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinForPlayer(player.getName());
		skinprofile.applySkin(new SkinProfile.ApplyFunction() {
			@Override
			public void applySkin(SkinProperty property) {
				try {
					Property prop = null;

					prop = new Property(property.getName(), property.getValue(), property.getSignature());

					Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);

					Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");

					GameProfile profile = (GameProfile) ReflectionUtil
							.invokeMethod(ReflectionUtil.getNMSClass("EntityPlayer"), ep, "getProfile");

					// Clear the current textures (skin & cape).
					profile.getProperties().get(prop.getName()).clear();

					// Putting the new one.
					profile.getProperties().get(prop.getName()).add(prop);

					// Updating skin.
					updateSkin(player);

				} catch (NoClassDefFoundError e) {
					SkinsRestorer.getInstance().getColoredLog()
							.sendMessage(ChatColor.RED + "[SkinsRestorer] The version "
									+ ReflectionUtil.getServerVersion() + " is not supported.");
					Bukkit.getPluginManager().disablePlugin(SkinsRestorer.getInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// Remove skin from player
	public void removeSkin(final Player player) {
		try {
			Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);

			Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");

			GameProfile profile = (GameProfile) ReflectionUtil.invokeMethod(ReflectionUtil.getNMSClass("EntityPlayer"),
					ep, "getProfile");
			profile.getProperties().get("textures").clear();
			updateSkin(player); // Removing the skin.
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Update the skin without reloging (Invoking player info packets with
	// reflection)
	@SuppressWarnings("deprecation")
	public void updateSkin(Player player) {
		try {
			Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
			Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");
			Location l = player.getLocation();

			ArrayList<Object> list = new ArrayList<Object>();
			list.add(ep);
			Iterable<?> iterable = list;

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

			Object world = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getWorld");
			Object difficulty = ReflectionUtil.invokeMethod(world.getClass(), world, "getDifficulty");
			Object worlddata = ReflectionUtil.getField(world.getClass(), "worldData").get(world);
			Object worldtype = ReflectionUtil.invokeMethod(worlddata.getClass(), worlddata, "getType");
			Object worldprovider = ReflectionUtil.getField(world.getClass(), "worldProvider").get(world);
			Object dimensionmanager = ReflectionUtil.invokeMethod(worldprovider.getClass(), worldprovider,
					"getDimensionManager");

			Object respawn = ReflectionUtil
					.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutRespawn"),
							new Class<?>[] { int.class,
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumDifficulty"), "PEACEFUL")
											.getClass(),
									worldtype.getClass(),
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("WorldSettings"), "EnumGamemode",
											"SURVIVAL").getClass() },
							ReflectionUtil
									.invokeMethod(dimensionmanager.getClass(), dimensionmanager, "getDimensionID"),
							difficulty, worldtype,
							ReflectionUtil.invokeMethod(
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("WorldSettings"), "EnumGamemode",
											"SURVIVAL").getClass(),
									null, "getById", new Class<?>[] { int.class }, player.getGameMode().getValue()));

			Object pos = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutPosition"),
					new Class<?>[] { double.class, double.class, double.class, float.class, float.class, Set.class,
							int.class },
					l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<Enum<?>>(), 0);

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
			}

			// And use packet definitons respective for these versions
			if (constr != null) {

				hand = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 0,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class }, player.getItemInHand()));

				helmet = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 4,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class }, player.getInventory().getHelmet()));

				chestplate = ReflectionUtil
						.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
								new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
								player.getEntityId(), 3,
								ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"),
										null, "asNMSCopy", new Class<?>[] { ItemStack.class },
										player.getInventory().getLeggings()));

				leggings = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 2,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class }, player.getInventory().getLeggings()));

				boots = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class, int.class, ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(), 1,
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class }, player.getInventory().getBoots()));
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
								ReflectionUtil.invokeMethod(ep.getClass(), ep, "getItemInMainHand")));

				offhand = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class,
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND")
										.getClass(),
								ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(),
						ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND"),
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								ReflectionUtil.invokeMethod(ep.getClass(), ep, "getItemInOffHand")));

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
										player.getInventory().getHelmet()));

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
										player.getInventory().getChestplate()));

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
										player.getInventory().getLeggings()));

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
										player.getInventory().getBoots()));
			}

			Object slot = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutHeldItemSlot"),
					new Class<?>[] { int.class }, player.getInventory().getHeldItemSlot());

			Object playerCons = ReflectionUtil.getField(ep.getClass(), "playerConnection").get(ep);

			ReflectionUtil.invokeMethod(playerCons.getClass(), playerCons, "sendPacket",
					new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, removeInfo);
			ReflectionUtil.invokeMethod(playerCons.getClass(), playerCons, "sendPacket",
					new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, addInfo);
			ReflectionUtil.invokeMethod(playerCons.getClass(), playerCons, "sendPacket",
					new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, respawn);
			ReflectionUtil.invokeMethod(playerCons.getClass(), playerCons, "sendPacket",
					new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, pos);
			ReflectionUtil.invokeMethod(playerCons.getClass(), playerCons, "sendPacket",
					new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, slot);
			ReflectionUtil.invokeMethod(cp.getClass(), cp, "updateScaledHealth");
			ReflectionUtil.invokeMethod(cp.getClass(), cp, "updateInventory");
			ReflectionUtil.invokeMethod(ep.getClass(), ep, "triggerHealthUpdate");

			Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {
					// This cant be async
					try {
						ReflectionUtil.invokeMethod(ep.getClass(), ep, "updateAbilities");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			});

			for (Object onlinep : (Collection<?>) ReflectionUtil.invokeMethod(Bukkit.class, null, "getOnlinePlayers")) {
				Player online = (Player) onlinep;
				Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
				Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
				Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(ep);

				Class<?> packet = ReflectionUtil.getNMSClass("Packet");

				if (online.getName().equals(player.getName())) {
					continue;
				}

				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						removeEntity);
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						removeInfo);
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						addInfo);
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						addNamed);

				if (hand == null) {
					ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket",
							new Class<?>[] { packet }, mainhand);
					ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket",
							new Class<?>[] { packet }, offhand);
				} else {
					ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket",
							new Class<?>[] { packet }, hand);
				}
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						helmet);
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						chestplate);
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						leggings);
				ReflectionUtil.invokeMethod(playerCon.getClass(), playerCon, "sendPacket", new Class<?>[] { packet },
						boots);
			}
		} catch (Exception e) {
			// Player logging in isnt finished and the method will not be used.
			// Player skin is already applied.
		}

	}
}
