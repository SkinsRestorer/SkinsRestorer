package skinsrestorer.bukkit.skinfactory;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EnumGamemode;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_7_R4.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_7_R4.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_7_R4.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_7_R4.PacketPlayOutPosition;
import net.minecraft.server.v1_7_R4.PacketPlayOutRespawn;
import net.minecraft.server.v1_7_R4.PlayerConnection;
import net.minecraft.server.v1_7_R4.WorldServer;
import skinsrestorer.bukkit.SkinsRestorer;

public class SkinFactory_v1_7_R4 extends SkinFactory {

	@SuppressWarnings("deprecation")
	@Override
	public void updateSkin(Player p) {
		try {
			if (!p.isOnline()) {
				return;
			}
			CraftPlayer cp = (CraftPlayer) p;
			EntityPlayer ep = cp.getHandle();
			int entId = ep.getId();
			Location l = p.getLocation();

			PacketPlayOutPlayerInfo removeInfo = PacketPlayOutPlayerInfo.removePlayer(ep);

			PacketPlayOutEntityDestroy removeEntity = new PacketPlayOutEntityDestroy(entId);

			PacketPlayOutNamedEntitySpawn addNamed = new PacketPlayOutNamedEntitySpawn(ep);

			PacketPlayOutPlayerInfo addInfo = PacketPlayOutPlayerInfo.addPlayer(ep);

			PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(((WorldServer) ep.getWorld()).dimension,
					ep.getWorld().difficulty, ep.getWorld().worldData.getType(),
					EnumGamemode.getById(p.getGameMode().getValue()));

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

			for (Player pOnline : ((CraftServer) Bukkit.getServer()).getOnlinePlayers()) {
				final CraftPlayer craftOnline = (CraftPlayer) pOnline;
				PlayerConnection con = craftOnline.getHandle().playerConnection;
				if (pOnline.equals(p)) {
					con.sendPacket(removeInfo);
					con.sendPacket(addInfo);
					con.sendPacket(respawn);
					con.sendPacket(pos);
					con.sendPacket(slot);
					craftOnline.updateScaledHealth();
					craftOnline.getHandle().triggerHealthUpdate();
					craftOnline.updateInventory();
					if (pOnline.isOp()) {
						//Fix for the command blocks bug.
						pOnline.setOp(false);
						pOnline.setOp(true);
					}
					Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {

						@Override
						public void run() {
							craftOnline.getHandle().updateAbilities();
							
						}

					});
					continue;
				}
				/*Now checks if the player is in the same world and if can see the player
				 * I did that to try to prevent player duplications.
				 */
				if (pOnline.canSee(p)&&pOnline.getWorld().equals(p.getWorld())) {
					con.sendPacket(removeEntity);
					con.sendPacket(removeInfo);
					con.sendPacket(addInfo);
					con.sendPacket(addNamed);
					con.sendPacket(itemhand);
					con.sendPacket(helmet);
					con.sendPacket(chestplate);
					con.sendPacket(leggings);
					con.sendPacket(boots);
				}else{
					//Only sends player update packet
					con.sendPacket(removeInfo);
					con.sendPacket(addInfo);
				}
			}
		} catch (Exception e) {
		}

	}

}
