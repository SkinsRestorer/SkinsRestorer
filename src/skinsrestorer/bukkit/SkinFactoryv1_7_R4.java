package skinsrestorer.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EnumGamemode;
import net.minecraft.server.v1_7_R4.PacketPlayOutAbilities;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_7_R4.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_7_R4.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_7_R4.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_7_R4.PacketPlayOutRespawn;
import net.minecraft.server.v1_7_R4.PlayerAbilities;
import net.minecraft.server.v1_7_R4.PlayerConnection;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;

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
				updateSkin(player, eplayer);

			}
		});
	}

	// Remove skin from player
	@Override
	public void removeSkin(final Player player) {
		GameProfile profile = ((CraftPlayer) player).getProfile();
		profile.getProperties().clear();
		updateSkin(player, profile); // Removing the skin.
	}

	// Update the skin without relog. (Using NMS and OBC)
	@Override
	@SuppressWarnings("deprecation")
	public void updateSkin(Player player, final GameProfile profile) {
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
			for (Player online : Bukkit.getOnlinePlayers()) {
				CraftPlayer craftOnline = (CraftPlayer) online;
                PlayerConnection playerCon = craftOnline.getHandle().playerConnection;
				if (online.equals(player)) {
					playerCon.sendPacket(removeInfo);
					playerCon.sendPacket(addInfo);
					playerCon.sendPacket(respawn);
					playerCon.sendPacket(packetAbilities);
					playerCon.sendPacket(slot);
					craftOnline.updateScaledHealth();
					craftOnline.updateInventory();
					continue;
				}
				playerCon.sendPacket(removeEntity);
				playerCon.sendPacket(removeInfo);
				playerCon.sendPacket(addInfo);
				playerCon.sendPacket(addNamed);
				playerCon.sendPacket(itemhand);
				playerCon.sendPacket(helmet);
				playerCon.sendPacket(chestplate);
				playerCon.sendPacket(leggings);
				playerCon.sendPacket(boots);
			}
		} catch (Exception e) {
			// Player logging in isnt finished and the method will not be used.
			// Player skin is already applied.
		}
	}

	// Just adding that, so the class will not be abstract. It will never be
	// used.
	@Override
	public void updateSkin(Player player, com.mojang.authlib.GameProfile profile) {
	}
}