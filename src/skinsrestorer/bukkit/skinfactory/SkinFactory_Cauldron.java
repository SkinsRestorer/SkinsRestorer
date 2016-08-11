package skinsrestorer.bukkit.skinfactory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.world.WorldSettings;
import skinsrestorer.bukkit.SkinsRestorer;

import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S38PacketPlayerListItem.Action;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import net.minecraft.util.com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.world.WorldServer;


/*I'm still not using that since i cna't really figure out how to
 * convert org.bukkit.entity.Player to net.minecraft.entity.player.EntityPlayerMP
 * 
 *  If someone knows please help.
 */
public class SkinFactory_Cauldron implements SkinFactory {

	@Override
	public void applySkin(Player p, Object props) {
		PropertyMap propmap = ((CraftPlayer) p).getProfile().getProperties();
		propmap.get("textures").clear();
		propmap.put("textures", (Property) props);
	}

	@Override
	public void removeOnQuit(Player p) {
		// TODO Create this.
		
	}

	@Override
	public void updateSkin(Player p) {
		try {
			EntityPlayerMP ep = (EntityPlayerMP) p; //TODO Fix that (But i can't)
			int entId = ep.getEntityId();
			Location l = p.getLocation();

			S38PacketPlayerListItem removeInfo = new S38PacketPlayerListItem(Action.REMOVE_PLAYER, ep);

			S13PacketDestroyEntities removeEntity = new S13PacketDestroyEntities(entId);

			S0CPacketSpawnPlayer addNamed = new S0CPacketSpawnPlayer(ep);

			S38PacketPlayerListItem addInfo = new S38PacketPlayerListItem(Action.ADD_PLAYER, ep);

			S07PacketRespawn respawn = new S07PacketRespawn(((WorldServer) ep.getEntityWorld()).provider.getDimensionId(),
					ep.getEntityWorld().getDifficulty(), ep.getEntityWorld().getWorldType(),
					WorldSettings.GameType.getByID(p.getGameMode().getValue()));

			S08PacketPlayerPosLook pos = new S08PacketPlayerPosLook(l.getX(), l.getY(), l.getZ(), l.getYaw(),
					l.getPitch(), null);

			S04PacketEntityEquipment itemhand = new S04PacketEntityEquipment(entId, 0,
					ItemStack.copyItemStack(ep.getHeldItem()));

			S04PacketEntityEquipment helmet = new S04PacketEntityEquipment(entId, 4,
					ItemStack.copyItemStack(ep.inventory.armorItemInSlot(0)));

			S04PacketEntityEquipment chestplate = new S04PacketEntityEquipment(entId, 3,
					ItemStack.copyItemStack(ep.inventory.armorItemInSlot(1)));

			S04PacketEntityEquipment leggings = new S04PacketEntityEquipment(entId, 2,
					ItemStack.copyItemStack(ep.inventory.armorItemInSlot(2)));

			S04PacketEntityEquipment boots = new S04PacketEntityEquipment(entId, 1,
					ItemStack.copyItemStack(ep.inventory.armorItemInSlot(3)));

			S09PacketHeldItemChange slot = new S09PacketHeldItemChange(p.getInventory().getHeldItemSlot());

			for (Player inWorld : p.getWorld().getPlayers()) {
				final EntityPlayerMP craftOnline = (EntityPlayerMP) inWorld;  //TODO Fix that (But i can't)
				NetHandlerPlayServer con = craftOnline.playerNetServerHandler;
				if (inWorld.equals(p)) {
					con.sendPacket(removeInfo);
					con.sendPacket(addInfo);
					con.sendPacket(respawn);
					con.sendPacket(pos);
					con.sendPacket(slot);
					craftOnline.onUpdate();

					Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {

						@Override
						public void run() {
							craftOnline.sendPlayerAbilities();
						}

					});
					continue;
				}
				con.sendPacket(removeEntity);
				con.sendPacket(removeInfo);
				if (inWorld.canSee(p)){
				con.sendPacket(addInfo);
				con.sendPacket(addNamed);
				con.sendPacket(itemhand);
				con.sendPacket(helmet);
				con.sendPacket(chestplate);
				con.sendPacket(leggings);
				con.sendPacket(boots);
				}
			}
		} catch (Exception e) {
		}

	}

}