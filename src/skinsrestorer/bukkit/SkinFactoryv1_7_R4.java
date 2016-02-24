package skinsrestorer.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EnumGamemode;
import net.minecraft.server.v1_7_R4.PacketPlayOutAbilities;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_7_R4.PacketPlayOutExperience;
import net.minecraft.server.v1_7_R4.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_7_R4.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_7_R4.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_7_R4.PacketPlayOutRespawn;
import net.minecraft.server.v1_7_R4.PacketPlayOutUpdateHealth;
import net.minecraft.server.v1_7_R4.PlayerAbilities;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;

public class SkinFactoryv1_7_R4 extends Factory {

	public static SkinFactoryv1_7_R4 skinfactory;

	public SkinFactoryv1_7_R4() {
		skinfactory = this;
	}

	public static SkinFactoryv1_7_R4 getFactory() {
		return skinfactory;
	}

	// Apply the skin to the player.
	@Override
	public void applySkin(final Player player) {
		SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(player.getName().toLowerCase());
		skinprofile.applySkin(new SkinProfile.ApplyFunction() {
			@Override
			public void applySkin(SkinProperty property) {
				EntityPlayer ep = ((CraftPlayer) player).getHandle();
				GameProfile eplayer = ep.getProfile();
				Property prop = new Property(property.getName(), property.getValue(), property.getSignature());

				// Clear the current textures (skin & cape).
				eplayer.getProperties().get("textures").clear();
				// Putting the new one.
				eplayer.getProperties().put(prop.getName(), prop);
				// Updating skin.
				updateSkin(player, eplayer, false);

			}
		});
	}

	// Remove skin from player
	@Override
	public void removeSkin(final Player player) {
		GameProfile profile = ((CraftPlayer) player).getProfile();
		updateSkin(player, profile, true); // Removing the skin.
	}

	// Update the skin without relog. (Using NMS and OBC)
	@Override
	@SuppressWarnings("deprecation")
	public void updateSkin(Player player, final GameProfile profile, boolean removeSkin) {
		try {
			PacketPlayOutPlayerInfo removeInfo = PacketPlayOutPlayerInfo
					.removePlayer(((CraftPlayer) player).getHandle());
			PacketPlayOutEntityDestroy removeEntity = new PacketPlayOutEntityDestroy(
					new int[] { player.getEntityId() });
			PacketPlayOutNamedEntitySpawn addNamed = new PacketPlayOutNamedEntitySpawn(
					((CraftPlayer) player).getHandle());
			PacketPlayOutPlayerInfo addInfo = PacketPlayOutPlayerInfo.addPlayer(((CraftPlayer) player).getHandle());
			PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(
					((CraftPlayer) player).getHandle().getWorld().worldProvider.dimension,
					((CraftPlayer) player).getHandle().getWorld().difficulty,
					((CraftPlayer) player).getHandle().getWorld().worldData.getType(),
					EnumGamemode.getById(player.getGameMode().getValue()));
			PacketPlayOutEntityEquipment itemhand = new PacketPlayOutEntityEquipment(player.getEntityId(), 0, CraftItemStack.asNMSCopy(player.getItemInHand()));
			PacketPlayOutEntityEquipment helmet = new PacketPlayOutEntityEquipment(player.getEntityId(), 4, CraftItemStack.asNMSCopy(player.getInventory().getHelmet()));
			PacketPlayOutEntityEquipment chestplate = new PacketPlayOutEntityEquipment(player.getEntityId(), 3, CraftItemStack.asNMSCopy(player.getInventory().getChestplate()));
			PacketPlayOutEntityEquipment leggings = new PacketPlayOutEntityEquipment(player.getEntityId(), 2, CraftItemStack.asNMSCopy(player.getInventory().getLeggings()));
			PacketPlayOutEntityEquipment boots = new PacketPlayOutEntityEquipment(player.getEntityId(), 1, CraftItemStack.asNMSCopy(player.getInventory().getBoots()));
			PacketPlayOutHeldItemSlot slot = new PacketPlayOutHeldItemSlot(player.getInventory().getHeldItemSlot());
			PlayerAbilities abilities = ((CraftPlayer) player).getHandle().abilities;
			PacketPlayOutAbilities packetAbilities = new PacketPlayOutAbilities(abilities);
			PacketPlayOutExperience exp = new PacketPlayOutExperience(player.getExp(),0,player.getLevel());
			PacketPlayOutUpdateHealth health = new PacketPlayOutUpdateHealth((float) player.getHealth(), player.getFoodLevel(), player.getSaturation());
			for (Player online : Bukkit.getOnlinePlayers()) {
				CraftPlayer craftOnline = (CraftPlayer) online;
				if (online.equals(player)) {
					craftOnline.getHandle().playerConnection.sendPacket(removeInfo);
					if (removeSkin == false) {
						craftOnline.getHandle().playerConnection.sendPacket(addInfo);
					}
					craftOnline.getHandle().playerConnection.sendPacket(respawn);
					craftOnline.getHandle().playerConnection.sendPacket(packetAbilities);
					craftOnline.getHandle().playerConnection.sendPacket(slot);
					craftOnline.getHandle().playerConnection.sendPacket(exp);
					craftOnline.getHandle().playerConnection.sendPacket(health);
					craftOnline.updateInventory();
					Chunk chunk = player.getLocation().getChunk();
					player.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
					continue;
				}
				craftOnline.getHandle().playerConnection.sendPacket(removeEntity);
				craftOnline.getHandle().playerConnection.sendPacket(removeInfo);
				if (removeSkin == false) {
					craftOnline.getHandle().playerConnection.sendPacket(addInfo);
				}
				craftOnline.getHandle().playerConnection.sendPacket(addNamed);
				craftOnline.getHandle().playerConnection.sendPacket(itemhand);
				craftOnline.getHandle().playerConnection.sendPacket(helmet);
				craftOnline.getHandle().playerConnection.sendPacket(chestplate);
				craftOnline.getHandle().playerConnection.sendPacket(leggings);
				craftOnline.getHandle().playerConnection.sendPacket(boots);
				craftOnline.hidePlayer(player);
				craftOnline.showPlayer(player);
			}
		} catch (Exception e) {
			// Player logging in isnt finished and the method will not be used.
			// Player skin is already applied.
		}
	}

	// Just adding that, so the class will not be abstract. It will never be
	// used.
	@Override
	public void updateSkin(Player player, com.mojang.authlib.GameProfile profile, boolean removeSkin) {
	}

	/*
	 * I will keep these methods there, because i will probably use them again.
	 * private static Class<?> getCraftClass(String name) { String version =
	 * Bukkit.getServer().getClass().getPackage().getName().replace(".",
	 * ",").split(",")[3] + "."; String className = "org.bukkit.craftbukkit." +
	 * version + name; Class<?> clazz = null; try { clazz =
	 * Class.forName(className); } catch (ClassNotFoundException e) {
	 * e.printStackTrace(); } return clazz; }
	 * 
	 * @SuppressWarnings("unused") private static Class<?> getNMSClass(String
	 * name) { String version =
	 * Bukkit.getServer().getClass().getPackage().getName().replace(".",
	 * ",").split(",")[3] + "."; String className = "net.minecraft.server." +
	 * version + name; Class<?> clazz = null; try { clazz =
	 * Class.forName(className); } catch (ClassNotFoundException e) {
	 * e.printStackTrace(); } return clazz; }
	 * 
	 * protected static void setValue(Object owner, Field field, Object value)
	 * throws Exception { makeModifiable(field); field.set(owner, value); }
	 * 
	 * protected static void makeModifiable(Field nameField) throws Exception {
	 * nameField.setAccessible(true); int modifiers = nameField.getModifiers();
	 * Field modifierField = nameField.getClass().getDeclaredField("modifiers");
	 * modifiers = modifiers & ~Modifier.FINAL;
	 * modifierField.setAccessible(true); modifierField.setInt(nameField,
	 * modifiers); }
	 * 
	 * @SuppressWarnings("unused") private Object getValue(Object instance,
	 * String field) throws Exception { Field f =
	 * instance.getClass().getDeclaredField(field); f.setAccessible(true);
	 * return f.get(instance); }
	 */
}