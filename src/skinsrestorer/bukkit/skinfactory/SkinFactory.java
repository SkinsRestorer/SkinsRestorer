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
	 * @param propertymap
	 *            - Additional property map where props should be applied. Set
	 *            to null if none. We use this for the propertymap inside
	 *            PlayerInfo packet.
	 */
	public void applySkin(Player p, Object props, Object propertymap);

	/**
	 * Instantly updates player's skin
	 * 
	 * @param p
	 *            - Player
	 */
	public void updateSkin(Player p);

}
