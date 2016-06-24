package skinsrestorer.shared.api;

import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.SkinsPacketHandler;
import skinsrestorer.shared.utils.YamlConfig;

public class SkinsRestorerAPI {

	/**
	 * This method is used to set player's skin.
	 * <p>
	 * Keep in mind it just sets the skin, <b>you have to apply the skin using
	 * another method! </b>
	 * <p>
	 * Method will not do anything if it fails to get the skin from MojangAPI or
	 * database!
	 * 
	 * @param playerName
	 *            = Player's nick name
	 * 
	 * @param skinName
	 *            = Skin's name
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
	 * 
	 * @param playerName
	 *            = Player's nick name
	 */
	public static boolean hasSkin(String playerName) {
		return SkinStorage.getInstance().getPlayerSkin(playerName) != null;
	}

	/**
	 * This method is used to get player's skin name.
	 * 
	 * When player has no skin OR his skin name equals his username, returns
	 * null (this is because of cache clean ups)
	 * 
	 * @param playerName
	 *            = Player's nick name
	 */
	public static String getSkinName(String playerName) {
		return SkinStorage.getInstance().getPlayerSkin(playerName);
	}

	/**
	 * Used for instant skin applying.
	 * 
	 * @param player
	 *            = Player's instance (either ProxiedPlayer or Player)
	 */
	public static void applySkin(Object player) {
		try {
			ReflectionUtil.invokeMethod(SkinsPacketHandler.class, null, "updateSkin",
					new Class<?>[] { Class.forName("org.bukkit.entity.Player") }, player);
		} catch (Throwable t) {
			try {
				Object sr = ReflectionUtil.invokeMethod(Class.forName("skinsrestorer.bungee.SkinsRestorer"), null,
						"getInstance");
				Object factory = ReflectionUtil.invokeMethod(sr.getClass(), sr, "getFactory");

				ReflectionUtil.invokeMethod(factory.getClass(), factory, "applySkin",
						new Class<?>[] { Class.forName("net.md_5.bungee.api.connection.ProxiedPlayer") }, player);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Used for instant skin applying. This method can be used on Bungeecord
	 * side only!
	 */
	public static void applySkin(net.md_5.bungee.api.connection.ProxiedPlayer player) {
		skinsrestorer.bungee.SkinsRestorer.getInstance().getFactory().applySkin(player);
	}

	/**
	 * Used to remove player's skin.
	 * 
	 * You have to use apply method if you want instant results.
	 * 
	 * @param playername
	 *            = Player's nick name
	 * 
	 */
	public static void removeSkin(String playername) {
		SkinStorage.getInstance().removePlayerSkin(playername);
	}

	/**
	 * Used to get the SkinsRestorer config if needed for external plugins which
	 * are depending on SkinsRestorer
	 */
	public static YamlConfig getConfig() {
		return ConfigStorage.getInstance().config;
	}
}