package skinsrestorer.bukkit;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_8_R2.EntityPlayer;
import net.minecraft.server.v1_8_R2.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R2.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_8_R2.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R2.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R2.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_8_R2.PacketPlayOutPosition;
import net.minecraft.server.v1_8_R2.PacketPlayOutPosition.EnumPlayerTeleportFlags;
import net.minecraft.server.v1_8_R2.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R2.WorldSettings.EnumGamemode;
import net.minecraft.server.v1_8_R2.PacketPlayOutEntityEquipment;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;

public class SkinFactoryv1_8_R2 extends Factory {

	public static SkinFactoryv1_8_R2 skinfactory;

	public SkinFactoryv1_8_R2() {
		skinfactory = this;
	}

	public static SkinFactoryv1_8_R2 getFactory() {
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
				com.mojang.authlib.GameProfile eplayer = ep.getProfile();
				com.mojang.authlib.properties.Property prop = new com.mojang.authlib.properties.Property(
						property.getName(), property.getValue(), property.getSignature());

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
			PacketPlayOutEntityEquipment itemhand = new PacketPlayOutEntityEquipment(player.getEntityId(), 0, CraftItemStack.asNMSCopy(player.getItemInHand()));
			PacketPlayOutEntityEquipment helmet = new PacketPlayOutEntityEquipment(player.getEntityId(), 4, CraftItemStack.asNMSCopy(player.getInventory().getHelmet()));
			PacketPlayOutEntityEquipment chestplate = new PacketPlayOutEntityEquipment(player.getEntityId(), 3, CraftItemStack.asNMSCopy(player.getInventory().getChestplate()));
			PacketPlayOutEntityEquipment leggings = new PacketPlayOutEntityEquipment(player.getEntityId(), 2, CraftItemStack.asNMSCopy(player.getInventory().getLeggings()));
			PacketPlayOutEntityEquipment boots = new PacketPlayOutEntityEquipment(player.getEntityId(), 1, CraftItemStack.asNMSCopy(player.getInventory().getBoots()));
			PacketPlayOutHeldItemSlot slot = new PacketPlayOutHeldItemSlot(player.getInventory().getHeldItemSlot());
			for (Player online : Bukkit.getOnlinePlayers()) {
				CraftPlayer craftOnline = (CraftPlayer) online;
				if (online.equals(player)) {
					craftOnline.getHandle().playerConnection.sendPacket(removeInfo);
					if (removeSkin == false) {
						craftOnline.getHandle().playerConnection.sendPacket(addInfo);
					}
					craftOnline.getHandle().playerConnection.sendPacket(respawn);
					craftOnline.getHandle().playerConnection.sendPacket(pos);
					craftOnline.getHandle().playerConnection.sendPacket(slot);
					craftOnline.updateInventory();
					Chunk chunk = l.getChunk();
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
			}
		} catch (Exception e) {
			// Player logging in isnt finished and the method will not be used.
			// Player skin is already applied.
		}
	}

	// Just adding that, so the class will not be abstract. It will never be
	// used.
	@Override
	public void updateSkin(Player player, net.minecraft.util.com.mojang.authlib.GameProfile profile,
			boolean removeSkin) {
	}
}