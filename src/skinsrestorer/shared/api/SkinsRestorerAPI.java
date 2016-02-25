package skinsrestorer.shared.api;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.bungee.SkinFactoryBungee;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class SkinsRestorerAPI {
	/**
	 * This method is used to set player's skin.
	 * <p>
	 */
	public static void setSkin(final String playerName, final String skinName) throws SkinFetchFailedException {
		SkinProfile skinprofile = null;
		try {
			// TODO: This needs to be done async! Leaving it be for now
			skinprofile = SkinFetchUtils.fetchSkinProfile(skinName, null);
			if (skinsrestorer.bungee.SkinStorage.getInstance() != null)
				skinsrestorer.bungee.SkinStorage.getInstance().setSkinData(playerName, skinprofile);
			else
				skinsrestorer.bukkit.SkinStorage.getInstance().setSkinData(playerName, skinprofile);
		} catch (SkinFetchFailedException e) {

			if (skinsrestorer.bungee.SkinStorage.getInstance() != null) {
				skinprofile = skinsrestorer.bungee.SkinStorage.getInstance().getSkinData(skinName);

				if (skinprofile == null)
					throw e;

				skinsrestorer.bungee.SkinStorage.getInstance().setSkinData(playerName, skinprofile);

			} else {

				skinprofile = skinsrestorer.bukkit.SkinStorage.getInstance().getSkinData(skinName);

				if (skinprofile == null)
					throw e;

				skinsrestorer.bukkit.SkinStorage.getInstance().setSkinData(playerName, skinprofile);
			}
		}

	}

	/**
	 * This method is used to check if player has saved skin data. If player
	 * skin data equals null, the method will return false. Else if player has
	 * saved data, it will return true.
	 */
	public static boolean hasSkin(String playerName) {
		if (skinsrestorer.bungee.SkinStorage.getInstance() != null) {
			if (skinsrestorer.bungee.SkinStorage.getInstance().getSkinData(playerName) == null) {
				return false;
			}
			return true;
		} else {
			if (skinsrestorer.bungee.SkinStorage.getInstance().getSkinData(playerName) == null) {
				return false;
			}
			return true;
		}
	}

	/**
	 * This method is used to get player's skin name. If player doesn't have
	 * skin, the method will return null. Else it will return player's skin
	 * name.
	 */
	public static String getSkinName(String playerName) {
		if (skinsrestorer.bungee.SkinStorage.getInstance() != null) {

			SkinProfile data = skinsrestorer.bungee.SkinStorage.getInstance().getSkinData(playerName);
			if (data == null) {
				return null;
			}
			return data.getName();

		} else {
			SkinProfile data = skinsrestorer.bukkit.SkinStorage.getInstance().getSkinData(playerName);
			if (data == null) {
				return null;
			}
			return data.getName();
		}
	}

	/**
	 * Used for instant skin applying. Since i'm using NMS and OBC this method
	 * can be used only in bukkit/spigot but not on bungeecord.
	 */
	public static void applySkinBukkit(Player player) {
		SkinsRestorer.getInstance().getFactory().applySkin(player);
	}

	/**
	 * Used for instant skin applying. This method can be used on Bungeecord
	 * side only!
	 */
	public static void applySkinBungee(ProxiedPlayer player) {
		SkinFactoryBungee.getFactory().applySkin(player);
	}

	/**
	 * Used for instant skin removing. This method can be used on Bungeecord
	 * side only!
	 */
	public static void removeSkinBungee(ProxiedPlayer player) {
		SkinFactoryBungee.getFactory().removeSkin(player);
	}

	/**
	 * Used for instant skin removing. Since i'm using NMS and OBC this method
	 * can be used only in bukkit/spigot but not on bungeecord.
	 */
	public static void removeSkinBukkit(Player player) {
		SkinsRestorer.getInstance().getFactory().removeSkin(player);
	}
}