package skinsrestorer.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import net.minecraft.server.v1_8_R2.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R2.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R2.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R2.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_8_R2.PacketPlayOutRespawn;
import net.minecraft.server.v1_8_R2.WorldSettings.EnumGamemode;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;

public class SkinFactory1_8_R2 {

	public static SkinFactory1_8_R2 skinfactory;
	public SkinFactory1_8_R2(){
		skinfactory = this;
	}
	public static SkinFactory1_8_R2 getFactory(){
		return skinfactory;
	}
	
	//Apply the skin to the player.
	public void applySkin(final Player player){
		 SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(player.getName().toLowerCase());
		skinprofile.applySkin(new SkinProfile.ApplyFunction() {
			@Override
			public void applySkin(SkinProperty property) {
				Class<?> clazz = getCraftClass("entity.CraftPlayer");
				try {
					Method getprofile = clazz.getMethod("getProfile");
					com.mojang.authlib.GameProfile eplayer = (com.mojang.authlib.GameProfile) getprofile.invoke(player);
					com.mojang.authlib.properties.Property prop = new com.mojang.authlib.properties.Property(property.getName(), property.getValue(), property.getSignature());
					 
					//Clear the current textures (skin & cape).
					eplayer.getProperties().get("textures").clear();
					
					//Putting the new one. 
					eplayer.getProperties().put(prop.getName(), prop);
					
					//Updating skin.
				    updateSkin(player, eplayer);
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
				}
			}
		});
	}
		  
    //Update the skin without relog. (Using NMS and OBC)
	@SuppressWarnings("deprecation")
	public void updateSkin(Player player, final com.mojang.authlib.GameProfile profile) {
		PacketPlayOutPlayerInfo removeInfo = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, ((CraftPlayer) player).getHandle());
        PacketPlayOutEntityDestroy removeEntity = new PacketPlayOutEntityDestroy(new int[] { player.getEntityId() });
        PacketPlayOutNamedEntitySpawn addNamed = new PacketPlayOutNamedEntitySpawn(((CraftPlayer) player).getHandle());
        PacketPlayOutPlayerInfo add = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, ((CraftPlayer) player).getHandle());
        PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(((CraftPlayer) player).getHandle().getWorld().worldProvider.getDimension(), ((CraftPlayer) player).getHandle().getWorld().getDifficulty(), ((CraftPlayer) player).getHandle().getWorld().G(), EnumGamemode.getById(player.getGameMode().getValue()));
        
        for(Player online : Bukkit.getOnlinePlayers()) {
            CraftPlayer craftOnline = (CraftPlayer) online;
            if (online.equals(player)){
                craftOnline.getHandle().playerConnection.sendPacket(removeInfo);
    	        craftOnline.getHandle().playerConnection.sendPacket(add);
    	        craftOnline.getHandle().playerConnection.sendPacket(respawn);
            Chunk chunk = player.getWorld().getChunkAt(player.getLocation());
            for(int x=-10; x<10; x++) for(int z=-10; z<10; z++)
                player.getWorld().refreshChunk(chunk.getX()+x, chunk.getZ()+z);
            	continue;
            }
            craftOnline.getHandle().playerConnection.sendPacket(removeEntity);
            craftOnline.getHandle().playerConnection.sendPacket(removeInfo);
	        craftOnline.getHandle().playerConnection.sendPacket(add);
            craftOnline.getHandle().playerConnection.sendPacket(addNamed);
        }
    }
    // Refletion stuff down there.
	private static Class<?> getCraftClass(String name) {
	String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
	String className = "org.bukkit.craftbukkit." + version + name;
	Class<?> clazz = null;
	try {
	clazz = Class.forName(className);
	} catch (ClassNotFoundException e) {
	e.printStackTrace();
	}
	return clazz;
	}
	@SuppressWarnings("unused")
	private static Class<?> getNMSClass(String name) {
	String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
	String className = "net.minecraft.server." + version + name;
	Class<?> clazz = null;
	try {
	clazz = Class.forName(className);
	} catch (ClassNotFoundException e) {
	e.printStackTrace();
	}
	return clazz;
	}
	
	  protected static void setValue(Object owner, Field field, Object value) throws Exception {
		    makeModifiable(field);
		    field.set(owner, value);
		  }

		  protected static void makeModifiable(Field nameField) throws Exception {
		    nameField.setAccessible(true);
		    int modifiers = nameField.getModifiers();
		    Field modifierField = nameField.getClass().getDeclaredField("modifiers");
		    modifiers = modifiers & ~Modifier.FINAL;
		    modifierField.setAccessible(true);
		    modifierField.setInt(nameField, modifiers);
		  }
		    @SuppressWarnings("unused")
			private Object getValue(Object instance, String field) throws Exception {
		        Field f = instance.getClass().getDeclaredField(field);
		        f.setAccessible(true);
		        return f.get(instance);
		    }
}