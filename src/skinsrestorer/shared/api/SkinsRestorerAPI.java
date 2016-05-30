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
	 * This method is used to check if player has saved skin data. If player
	 * skin data equals null, the method will return false. Else if player has
	 * saved data, it will return true.
	 */
	public static boolean hasSkin(String playerName) {
		if (SkinStorage.getInstance().getSkinData(playerName) == null) {
			return false;
		}
		return true;
	}

	/**
	 * This method is used to get player's skin name. If player doesn't have
	 * skin, the method will return null. Else it will return player's skin
	 * name.
	 */
	public static String getSkinName(String playerName) {

		SkinProfile data = SkinStorage.getInstance().getSkinData(playerName);
		if (data == null) {
			return null;
		}
		return data.getName();

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

	//////////////////// Deprecated Methods ////////////////////////
	@Deprecated
	public static void applySkinBukkit(org.bukkit.entity.Player player) {
		skinsrestorer.bukkit.SkinsRestorer.getInstance().getFactory().applySkin(player);
	}

	@Deprecated
	public static void applySkinBungee(net.md_5.bungee.api.connection.ProxiedPlayer player) {
		skinsrestorer.bungee.SkinFactoryBungee.getFactory().applySkin(player);
	}

	@Deprecated
	public static void removeSkinBungee(net.md_5.bungee.api.connection.ProxiedPlayer player) {
		skinsrestorer.bungee.SkinFactoryBungee.getFactory().removeSkin(player);
	}

	@Deprecated
	public static void removeSkinBukkit(org.bukkit.entity.Player player) {
		skinsrestorer.bukkit.SkinsRestorer.getInstance().getFactory().removeSkin(player);
	}
}