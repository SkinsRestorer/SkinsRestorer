package skinsrestorer.bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;

public class SkinFactory {

	public static void applySkin(final Player player){
		 SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(player.getName().toLowerCase());
		skinprofile.applySkin(new SkinProfile.ApplyFunction() {
			@Override
			public void applySkin(SkinProperty property) {
				Class<?> clazz = getCraftClass("entity.CraftPlayer");
				try {
					Method getprofile = clazz.getMethod("getProfile");
					if (SkinsRestorer.getInstance().is1_8()){
					com.mojang.authlib.GameProfile eplayer = (com.mojang.authlib.GameProfile) getprofile.invoke(player);
					com.mojang.authlib.properties.Property prop = new com.mojang.authlib.properties.Property(property.getName(), property.getValue(), property.getSignature());
					 eplayer.getProperties().put(prop.getName(), prop);
					}else if (SkinsRestorer.getInstance().is1_7()){
						net.minecraft.util.com.mojang.authlib.GameProfile eplayer = (net.minecraft.util.com.mojang.authlib.GameProfile) getprofile.invoke(player);
						net.minecraft.util.com.mojang.authlib.properties.Property prop = new net.minecraft.util.com.mojang.authlib.properties.Property(property.getName(), property.getValue(), property.getSignature());
						 eplayer.getProperties().put(prop.getName(), prop);
						}
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
				}
			}
		});
	}
	// Refletion
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
}