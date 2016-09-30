package skinsrestorer.bukkit.skinfactory;

import org.bukkit.entity.Player;

public interface SkinFactory {

	/**
	 * 
	 * Applies skin data from SkinStorage on a Player p.
	 * 
	 * 
	 * @param p
	 *            - Player
	 * @param props
	 *            - Property object
	 */
	public void applySkin(Player p, Object props);

	/**
	 * Instantly updates player's skin
	 * 
	 * @param p
	 *            - Player
	 */
	public void updateSkin(Player p);

}
