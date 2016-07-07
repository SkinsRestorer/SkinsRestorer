package skinsrestorer.bukkit.skinfactory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R3.EntityPlayer;
import net.minecraft.server.v1_7_R3.EnumGamemode;
import net.minecraft.server.v1_7_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_7_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_7_R3.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_7_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_7_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_7_R3.PacketPlayOutPosition;
import net.minecraft.server.v1_7_R3.PacketPlayOutRespawn;
import net.minecraft.server.v1_7_R3.PlayerConnection;
import net.minecraft.server.v1_7_R3.WorldServer;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import net.minecraft.util.com.mojang.authlib.properties.PropertyMap;
import skinsrestorer.bukkit.SkinsRestorer;

public class SkinFactory_v1_7_R3 implements SkinFactory {

	@Override
	public void applySkin(Player p, Object props) {
		PropertyMap propmap = ((CraftPlayer) p).getHandle().getProfile().getProperties();
		propmap.get("textures").clear();
		propmap.put("textures", (Property) props);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void updateSkin(Player p) {
		try {
			CraftPlayer cp = (CraftPlayer) p;
			EntityPlayer ep = cp.getHandle();
			int entId = ep.getId();
			Location l = p.getLocation();

			PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(ep.getProfile().getName(), false, entId);

			PacketPlayOutEntityDestroy removeEntity = new PacketPlayOutEntityDestroy(entId);

			PacketPlayOutNamedEntitySpawn addNamed = new PacketPlayOutNamedEntitySpawn(ep);

			PacketPlayOutPlayerInfo addInfo = new PacketPlayOutPlayerInfo(ep.getProfile().getName(), true, entId);

			PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(((WorldServer) ep.getWorld()).dimension,
					ep.getWorld().difficulty, ep.getWorld().worldData.getType(),
					EnumGamemode.a(p.getGameMode().getValue()));

			PacketPlayOutPosition pos = new PacketPlayOutPosition(l.getX(), l.getY(), l.getZ(), l.getYaw(),
					l.getPitch(), false);

			PacketPlayOutEntityEquipment itemhand = new PacketPlayOutEntityEquipment(entId, 0,
					CraftItemStack.asNMSCopy(p.getItemInHand()));

			PacketPlayOutEntityEquipment helmet = new PacketPlayOutEntityEquipment(entId, 4,
					CraftItemStack.asNMSCopy(p.getInventory().getHelmet()));

			PacketPlayOutEntityEquipment chestplate = new PacketPlayOutEntityEquipment(entId, 3,
					CraftItemStack.asNMSCopy(p.getInventory().getChestplate()));

			PacketPlayOutEntityEquipment leggings = new PacketPlayOutEntityEquipment(entId, 2,
					CraftItemStack.asNMSCopy(p.getInventory().getLeggings()));

			PacketPlayOutEntityEquipment boots = new PacketPlayOutEntityEquipment(entId, 1,
					CraftItemStack.asNMSCopy(p.getInventory().getBoots()));

			PacketPlayOutHeldItemSlot slot = new PacketPlayOutHeldItemSlot(p.getInventory().getHeldItemSlot());

			for (Player online : p.getWorld().getPlayers()) {
				CraftPlayer craftOnline = (CraftPlayer) online;
				PlayerConnection con = craftOnline.getHandle().playerConnection;
				if (online.equals(p)) {
					con.sendPacket(removeInfo);
					con.sendPacket(addInfo);
					con.sendPacket(respawn);
					con.sendPacket(pos);
					con.sendPacket(slot);
					craftOnline.updateScaledHealth();
					craftOnline.getHandle().triggerHealthUpdate();
					craftOnline.updateInventory();
					Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {

						@Override
						public void run() {
							craftOnline.getHandle().updateAbilities();
						}

					});
					continue;
				}
				con.sendPacket(removeEntity);
				con.sendPacket(removeInfo);
				con.sendPacket(addInfo);
				con.sendPacket(addNamed);
				con.sendPacket(itemhand);
				con.sendPacket(helmet);
				con.sendPacket(chestplate);
				con.sendPacket(leggings);
				con.sendPacket(boots);
			}
		} catch (Exception e) {
			e.printStackTrace(); // TODO: remove
		}

	}

}
