package skinsrestorer.shared.utils;

public class Factory {

	public Factory() {

	}

	// Using this factory I will not always check the version field so i can use
	// it.
	public void applySkin(org.bukkit.entity.Player player) {
	}

	public void applySkin(net.md_5.bungee.api.connection.ProxiedPlayer player) {
	}

	// For 1.8+ servers.
	public void updateSkin(org.bukkit.entity.Player player, com.mojang.authlib.GameProfile profile) {
	}

	// For 1.7 servers.
	public void updateSkin(org.bukkit.entity.Player player, net.minecraft.util.com.mojang.authlib.GameProfile profile) {
	}

	// For Bungeecord
	public void updateSkin(net.md_5.bungee.api.connection.ProxiedPlayer player,
			net.md_5.bungee.connection.LoginResult result) {

	}

	// For CraftBukkit/spigot
	public void removeSkin(org.bukkit.entity.Player player) {
	}

	// For Bungeecord
	public void removeSkin(net.md_5.bungee.api.connection.ProxiedPlayer player) {
	}

}