package skinsrestorer.shared.api;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.minecraft.util.com.google.common.collect.Iterables;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

public class SkinsRestorerAPI {

	
	/**
	 * Used for instant skin applying.
	 *
	 * @param player
	 *            = Player's instance (either ProxiedPlayer or Player)
	 */
	public static void applySkin(Object player, Object props) {
		// Trying to use Bukkit.
		try {
			skinsrestorer.bukkit.SkinsRestorer.getInstance().getFactory().applySkin((org.bukkit.entity.Player) player, props);;
		} catch (Throwable t) {
			// On fail trying to use Bungee.
			skinsrestorer.bungee.SkinApplier.applySkin((net.md_5.bungee.api.connection.ProxiedPlayer) player);
		}
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
		return SkinStorage.getPlayerSkin(playerName);
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
	public static Object getSkin(String skinName) {
		return SkinStorage.getSkinData(skinName);
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
		return SkinStorage.getPlayerSkin(playerName) != null;
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
		SkinStorage.removePlayerSkin(playername);
	}

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
		try {
			new Thread(new Runnable() {

				@Override
				public void run() {

					try {
					MojangAPI.getUUID(skinName);
					SkinStorage.setPlayerSkin(playerName, skinName);
					SkinStorage.setSkinData(playerName, SkinStorage.getOrCreateSkinForPlayer(skinName));
					
				} catch (SkinRequestException e) {
					e.printStackTrace();
				}
				}

			}).run();
		} catch (Throwable t) {
			org.bukkit.entity.Player p = null;

			try {
				p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
			} catch (Exception e) {
				p = Bukkit.getOnlinePlayers().iterator().next();
			}

			if (p != null) {
				ByteArrayOutputStream b = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(b);

				try {
					out.writeUTF("SkinsRestorer");
					out.writeUTF(playerName);
					out.writeUTF(skinName);

					p.sendPluginMessage(skinsrestorer.bukkit.SkinsRestorer.getInstance(), "BungeeCord",
							b.toByteArray());

					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}