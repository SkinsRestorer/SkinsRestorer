package skinsrestorer.bukkit.listeners;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

/**
 * This class handler all the stuff about skin applying
 * 
 * <p>
 * How it works:
 * <p>
 * 1. Server tries to send PacketPlayOutPlayerInfo packet to a player
 * <p>
 * 2. This handler catches it before default packet_handler
 * <p>
 * 3. Checks if its adding player (that when client takes skin properties in
 * account)
 * <p>
 * 4. Checks if the included game profiles are profiles of online players (not
 * NPCs)
 * <p>
 * 5. Changes the properties for that player
 * <p>
 * 6. Sends the packet to the next handler (packet_handler)
 * <p>
 * 7. But since we have edited the properties in the packet, client will
 * instantly see skinned player (not steve, unless, there is no skin data)
 * <p>
 * 
 * @author Blackfire62
 * 
 **/

public class SkinsPacketHandler extends ChannelDuplexHandler {

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try {

			if (ReflectionUtil.getNMSClass("PacketPlayOutPlayerInfo").isInstance(msg)) {

				Enum<?> action = (Enum<?>) ReflectionUtil.getPrivateField(msg.getClass(), "a").get(msg);

				if (action.name().equalsIgnoreCase("ADD_PLAYER")) {

					List<?> dataList = (List<?>) ReflectionUtil.getPrivateField(msg.getClass(), "b").get(msg);

					// Making sure the skins are only applied
					// To online players, not NPCs or whatever
					for (Object plData : dataList) {
						Object profile = ReflectionUtil.invokeMethod(plData.getClass(), plData, "a");

						// Thread safe iterating

						for (Player p : Bukkit.getOnlinePlayers()) {

							if (p.getName()
									.equals(ReflectionUtil.invokeMethod(profile.getClass(), profile, "getName"))) {
								Object prop = SkinStorage.getOrCreateSkinForPlayer(p.getName());

								SkinsRestorer.getInstance().applyToGameProfile(profile, prop);

								super.write(ctx, msg, promise);
								return;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.write(ctx, msg, promise);
	}

	public static void inject(Player p) {
		try {
			Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(p);
			Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "channel").get(manager);

			if (channel.pipeline().context("skins_handler") != null)
				channel.pipeline().remove("skins_handler");

			if (channel.pipeline().context("skins_handler") == null)
				channel.pipeline().addBefore("packet_handler", "skins_handler", new SkinsPacketHandler());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void uninject(Player p) {
		try {
			Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(p);
			Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
			Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
			Object manager = ReflectionUtil.getField(playerCon.getClass(), "networkManager").get(playerCon);
			Channel channel = (Channel) ReflectionUtil.getField(manager.getClass(), "channel").get(manager);

			if (channel.pipeline().context("skins_handler") != null)
				channel.pipeline().remove("skins_handler");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Update the skin without reloging
	// Omg so much reflection
	// Im lazy to make so much classes for just NMS
	@SuppressWarnings("deprecation")
	public static void updateSkin(Player player) {
		try {
			Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
			Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");
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
			Object world = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getWorld");
			Object difficulty = ReflectionUtil.invokeMethod(world.getClass(), world, "getDifficulty");
			Object worlddata = ReflectionUtil.getField(world.getClass(), "worldData").get(world);
			Object worldtype = ReflectionUtil.invokeMethod(worlddata.getClass(), worlddata, "getType");
			Object worldprovider = ReflectionUtil.getField(world.getClass(), "worldProvider").get(world);

			Object dimensionmanager = null;
			int dimension = -1;
			try {
				dimensionmanager = ReflectionUtil.invokeMethod(worldprovider.getClass(), worldprovider,
						"getDimensionManager");
				dimension = (int) ReflectionUtil.invokeMethod(dimensionmanager.getClass(), dimensionmanager,
						"getDimensionID");
			} catch (Exception e) {
				dimension = ReflectionUtil.getField(worldprovider.getClass(), "dimension").getInt(worldprovider);
			}
			Enum<?> enumGamemode = null;

			try {
				// 1.7 - 1.9
				enumGamemode = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("WorldSettings"), "EnumGamemode",
						"SURVIVAL");

			} catch (Throwable t) {
				// 1.10 +
				enumGamemode = ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumGamemode"), "SURVIVAL");
			}

			int gmid = 0;

			try {
				gmid = (int) ReflectionUtil.invokeMethod(player.getGameMode().getClass(), player.getGameMode(),
						"getValue");
			} catch (Exception e) {
			}

			Object respawn = ReflectionUtil
					.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutRespawn"),
							new Class<?>[] { int.class,
									ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumDifficulty"), "PEACEFUL")
											.getClass(),
									worldtype.getClass(), enumGamemode.getClass() },
							dimension, difficulty, worldtype, ReflectionUtil.invokeMethod(enumGamemode.getClass(), null,
									"getById", new Class<?>[] { int.class }, gmid));

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
				// If 1.9+, leave it at null
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
								player.getInventory().getItemInMainHand()));

				offhand = ReflectionUtil.invokeConstructor(ReflectionUtil.getNMSClass("PacketPlayOutEntityEquipment"),
						new Class<?>[] { int.class,
								ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND")
										.getClass(),
								ReflectionUtil.getNMSClass("ItemStack") },
						player.getEntityId(),
						ReflectionUtil.getEnum(ReflectionUtil.getNMSClass("EnumItemSlot"), "OFFHAND"),
						ReflectionUtil.invokeMethod(ReflectionUtil.getBukkitClass("inventory.CraftItemStack"), null,
								"asNMSCopy", new Class<?>[] { ItemStack.class },
								player.getInventory().getItemInOffHand()));

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

			// We finished defining packets, now lets send em

			for (Player online : Bukkit.getOnlinePlayers()) {
				Object craftOnline = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(online);
				final Object craftHandle = ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "getHandle");
				Object playerCon = ReflectionUtil.getField(craftHandle.getClass(), "playerConnection").get(craftHandle);
				if (online.equals(player)) {
					sendPacket(playerCon, removeInfo);
					sendPacket(playerCon, addInfo);
					sendPacket(playerCon, respawn);
					Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {
						@Override
						public void run() {
							// This cant be async
							// I may change this, it looks ugly
							try {
								ReflectionUtil.invokeMethod(craftHandle.getClass(), craftHandle, "updateAbilities");
							} catch (Exception e) {
							}
						}

					});
					sendPacket(playerCon, pos);
					sendPacket(playerCon, slot);
					ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "updateScaledHealth");
					ReflectionUtil.invokeMethod(craftOnline.getClass(), craftOnline, "updateInventory");
					ReflectionUtil.invokeMethod(craftHandle.getClass(), craftHandle, "triggerHealthUpdate");
					continue;
				}

				sendPacket(playerCon, removeEntity);
				sendPacket(playerCon, removeInfo);

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
		} catch (Exception e) {
			e.printStackTrace();
			// Ill just leave the printStackTrace here
			// So people can actually report errors
		}

	}

	private static void sendPacket(Object playerConnection, Object packet) throws Exception {
		ReflectionUtil.invokeMethod(playerConnection.getClass(), playerConnection, "sendPacket",
				new Class<?>[] { ReflectionUtil.getNMSClass("Packet") }, packet);
	}

}
