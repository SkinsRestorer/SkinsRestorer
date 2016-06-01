package skinsrestorer.shared.api;

import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.DataFiles;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class SkinsRestorerAPI {

	/**
	 * This method is used to set player's skin.
	 * <p>
	 * Keep in mind it just sets the skin, you have to apply it using another
	 * method!
	 * <p>
	 * Method will not do anything if it fails to get the skin from MojangAPI or
	 * database!
	 */
	public static void setSkin(final String playerName, final String skinName) {
		new Thread(new Runnable() {

			@Override
			public void run() {

				SkinProfile skinprofile = null;

				try {
					skinprofile = SkinFetchUtils.fetchSkinProfile(skinName, null);

					SkinStorage.getInstance().setSkinData(skinprofile);
					SkinStorage.getInstance().setPlayerSkin(playerName, skinprofile.getName());
				} catch (SkinFetchFailedException e) {

					skinprofile = SkinStorage.getInstance().getSkinData(skinName);

					if (skinprofile == null)
						return;

					SkinStorage.getInstance().setPlayerSkin(playerName, skinprofile.getName());
				}

			}

		}).run();
	}

	/**
	 * This method is used to check if player has set a skin. If player has no
	 * skin assigned (so playerName = skinName), the method will return false.
	 * Else if player has a skin assigned, returns true.
	 */
	public static boolean hasSkin(String playerName) {
		if (SkinStorage.getInstance().getPlayerSkin(playerName) == null) {
			return false;
		}
		return true;
	}

	/**
	 * This method is used to get player's skin name. If player doesn't have
	 * skin, the method will return null. Else it will return player's skin
	 * name.
	 * 
	 * When player has no skin (null) , his skin name equals his username
	 */
	public static String getSkinName(String playerName) {

		String skin = SkinStorage.getInstance().getPlayerSkin(playerName);
		if (skin == null) {
			return null;
		}
		return skin;

	}

	/**
	 * Used for instant skin applying. Since i'm using NMS and OBC this method
	 * can be used only in bukkit/spigot but not on bungeecord.
	 */
	public static void applySkin(org.bukkit.entity.Player player) {
		skinsrestorer.bukkit.SkinsRestorer.getInstance().getFactory().applySkin(player);
	}

	/**
	 * Used for instant skin applying. This method can be used on Bungeecord
	 * side only!
	 */
	public static void applySkin(net.md_5.bungee.api.connection.ProxiedPlayer player) {
		skinsrestorer.bungee.SkinsRestorer.getInstance().getFactory().applySkin(player);
	}

	/**
	 * Used for instant skin removing. This method can be used on Bungeecord
	 * side only!
	 */
	public static void removeSkin(net.md_5.bungee.api.connection.ProxiedPlayer player) {
		skinsrestorer.bungee.SkinsRestorer.getInstance().getFactory().removeSkin(player);
	}

	/**
	 * Used for instant skin removing. Since i'm using NMS and OBC this method
	 * can be used only in bukkit/spigot but not on bungeecord.
	 */
	public static void removeSkin(org.bukkit.entity.Player player) {
		skinsrestorer.bukkit.SkinsRestorer.getInstance().getFactory().removeSkin(player);
	}

	/**
	 * Used to get the SkinsRestorer config if needed for external plugins which
	 * are depending on SkinsRestorer
	 */
	public static DataFiles getConfig() {
		return ConfigStorage.getInstance().config;
	}
}