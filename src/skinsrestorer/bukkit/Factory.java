package skinsrestorer.bukkit;

import org.bukkit.entity.Player;

public class Factory {

	public Factory() {

	}

	// Using this factory I will not always check the version field so i can use
	// it.
	public void applySkin(Player player) {
	}

	// For 1.8+ servers.
	public void updateSkin(Player player, com.mojang.authlib.GameProfile profile, boolean removeSkin) {
	}

	// For 1.7 servers.
	public void updateSkin(Player player, net.minecraft.util.com.mojang.authlib.GameProfile profile,
			boolean removeSkin) {
	}

	public void removeSkin(Player player) {
	}

}