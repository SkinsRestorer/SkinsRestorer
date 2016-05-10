package skinsrestorer.bukkit;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_8_R2.EntityPlayer;
import net.minecraft.server.v1_8_R2.PacketPlayOutAbilities;
import net.minecraft.server.v1_8_R2.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R2.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R2.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_8_R2.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R2.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R2.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_8_R2.PacketPlayOutPosition;
import net.minecraft.server.v1_8_R2.PacketPlayOutPosition.EnumPlayerTeleportFlags;
import net.minecraft.server.v1_8_R2.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R2.PlayerAbilities;
import net.minecraft.server.v1_8_R2.PlayerConnection;
import net.minecraft.server.v1_8_R2.WorldSettings.EnumGamemode;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.Factory;

public class SkinFactoryv1_8_R2 extends Factory {

	public SkinFactoryv1_8_R2() {
	}

	// Apply the skin to the player.
	@Override
	public void applySkin(final Player player) {
		SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(player.getName().toLowerCase());
		skinprofile.applySkin(new SkinProfile.ApplyFunction() {
			@Override
			public void applySkin(SkinProperty property) {
				EntityPlayer ep = ((CraftPlayer) player).getHandle();
				com.mojang.authlib.GameProfile eplayer = ep.getProfile();
				com.mojang.authlib.properties.Property prop = new com.mojang.authlib.properties.Property(
						property.getName(), property.getValue(), property.getSignature());

				// Clear the current textures (skin & cape).
				eplayer.getProperties().get(prop.getName()).clear();
				// Putting the new one.
				eplayer.getProperties().get(prop.getName()).add(prop);
				// Updating skin.
				updateSkin(player, eplayer);

			}
		});
	}

	// Remove skin from player
	@Override
	public void removeSkin(final Player player) {
		GameProfile profile = ((CraftPlayer) player).getProfile();
		profile.getProperties().get("textures").clear();
		updateSkin(player, profile); // Removing the skin.
	}

	// Update the skin without relog. (Using NMS and OBC)
	@Override
	@SuppressWarnings("deprecation")
	public void updateSkin(Player player, GameProfile profile) {
		try {
			Location l = player.getLocation();
			PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER,
					((CraftPlayer) player).getHandle());
			PacketPlayOutEntityDestroy removeEntity = new PacketPlayOutEntityDestroy(
					new int[] { player.getEntityId() });
			PacketPlayOutNamedEntitySpawn addNamed = new PacketPlayOutNamedEntitySpawn(
					((CraftPlayer) player).getHandle());
			PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER,
					((CraftPlayer) player).getHandle());
			PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(
					((CraftPlayer) player).getHandle().getWorld().worldProvider.getDimension(),
					((CraftPlayer) player).getHandle().getWorld().getDifficulty(),
					((CraftPlayer) player).getHandle().getWorld().G(),
					EnumGamemode.getById(player.getGameMode().getValue()));
			PacketPlayOutPosition pos = new PacketPlayOutPosition(l.getX(), l.getY(), l.getZ(), l.getYaw(),
					l.getPitch(), new HashSet<EnumPlayerTeleportFlags>());
			PacketPlayOutEntityEquipment itemhand = new PacketPlayOutEntityEquipment(player.getEntityId(), 0,
					CraftItemStack.asNMSCopy(player.getItemInHand()));
			PacketPlayOutEntityEquipment helmet = new PacketPlayOutEntityEquipment(player.getEntityId(), 4,
					CraftItemStack.asNMSCopy(player.getInventory().getHelmet()));
			PacketPlayOutEntityEquipment chestplate = new PacketPlayOutEntityEquipment(player.getEntityId(), 3,
					CraftItemStack.asNMSCopy(player.getInventory().getChestplate()));
			PacketPlayOutEntityEquipment leggings = new PacketPlayOutEntityEquipment(player.getEntityId(), 2,
					CraftItemStack.asNMSCopy(player.getInventory().getLeggings()));
			PacketPlayOutEntityEquipment boots = new PacketPlayOutEntityEquipment(player.getEntityId(), 1,
					CraftItemStack.asNMSCopy(player.getInventory().getBoots()));
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
					playerCon.sendPacket(pos);
					playerCon.sendPacket(slot);
					craftOnline.updateScaledHealth();
					craftOnline.getHandle().triggerHealthUpdate();
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
}