package skinsrestorer.shared.interfaces;

import skinsrestorer.shared.exception.SkinRequestException;

/**
 * Created by McLive on 27.08.2019.
 *
 *  @param <P> Platform specific Player
 */
public interface ISkinsRestorerAPI<P> {
    String getUUID(String playerName) throws SkinRequestException;
    Object getProfile(String uuid);

    String getSkinName(String playerName);
    Object getSkinData(String skinName);

    boolean hasSkin(String playerName);

    void setSkinName(String playerName, String skinName);
    void removeSkin(String playername);

    void setSkin(String playerName, String skinName) throws SkinRequestException;

    void applySkin(P player, Object props);
    void applySkin(P player);
}
