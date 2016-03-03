package skinsrestorer.bukkit;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.minecraft.server.v1_9_R1.EntityPlayer;
import net.minecraft.server.v1_9_R1.EnumItemSlot;
import net.minecraft.server.v1_9_R1.PacketPlayOutAbilities;
import net.minecraft.server.v1_9_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_9_R1.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_9_R1.PacketPlayOutExperience;
import net.minecraft.server.v1_9_R1.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_9_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_9_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_9_R1.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_9_R1.PacketPlayOutPosition;
import net.minecraft.server.v1_9_R1.PacketPlayOutPosition.EnumPlayerTeleportFlags;
import net.minecraft.server.v1_9_R1.PacketPlayOutRespawn;
import net.minecraft.server.v1_9_R1.PacketPlayOutUpdateHealth;
import net.minecraft.server.v1_9_R1.PlayerAbilities;
import net.minecraft.server.v1_9_R1.WorldSettings.EnumGamemode;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;

public class SkinFactoryv1_9_R1 extends Factory {

	public static SkinFactoryv1_9_R1 skinfactory;

	public SkinFactoryv1_9_R1() {
		skinfactory = this;
	}

	public static SkinFactoryv1_9_R1 getFactory() {
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
	public void updateSkin(Player player, GameProfile profile, boolean removeSkin) {
		try {
			EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
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
					((CraftPlayer) player).getHandle().getWorld().worldProvider.getDimensionManager().getDimensionID(),
					((CraftPlayer) player).getHandle().getWorld().getDifficulty(),
					((CraftPlayer) player).getHandle().getWorld().L(),
					EnumGamemode.getById(player.getGameMode().getValue()));
			PacketPlayOutPosition pos = new PacketPlayOutPosition(l.getX(), l.getY(), l.getZ(), l.getYaw(),
					l.getPitch(), new HashSet<EnumPlayerTeleportFlags>(), 0);
			PacketPlayOutEntityEquipment mainhand = new PacketPlayOutEntityEquipment(player.getEntityId(), EnumItemSlot.MAINHAND, entityPlayer.getItemInMainHand());
			PacketPlayOutEntityEquipment offhand = new PacketPlayOutEntityEquipment(player.getEntityId(), EnumItemSlot.MAINHAND, entityPlayer.getItemInOffHand());
			PacketPlayOutEntityEquipment helmet = new PacketPlayOutEntityEquipment(player.getEntityId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(player.getInventory().getHelmet()));
			PacketPlayOutEntityEquipment chestplate = new PacketPlayOutEntityEquipment(player.getEntityId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(player.getInventory().getChestplate()));
			PacketPlayOutEntityEquipment leggings = new PacketPlayOutEntityEquipment(player.getEntityId(), EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(player.getInventory().getLeggings()));
			PacketPlayOutEntityEquipment boots = new PacketPlayOutEntityEquipment(player.getEntityId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(player.getInventory().getBoots()));
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
					craftOnline.getHandle().playerConnection.sendPacket(pos);
					craftOnline.getHandle().playerConnection.sendPacket(slot);
					craftOnline.getHandle().playerConnection.sendPacket(exp);
					craftOnline.getHandle().playerConnection.sendPacket(health);
					craftOnline.updateInventory();
					continue;
				}
				craftOnline.getHandle().playerConnection.sendPacket(removeEntity);
				craftOnline.getHandle().playerConnection.sendPacket(removeInfo);
				if (removeSkin == false) {
					craftOnline.getHandle().playerConnection.sendPacket(addInfo);
				}
				craftOnline.getHandle().playerConnection.sendPacket(addNamed);
				craftOnline.getHandle().playerConnection.sendPacket(mainhand);
				craftOnline.getHandle().playerConnection.sendPacket(offhand);
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