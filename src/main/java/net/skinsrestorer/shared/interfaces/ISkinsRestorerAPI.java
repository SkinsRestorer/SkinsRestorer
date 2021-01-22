package net.skinsrestorer.shared.interfaces;

import net.skinsrestorer.shared.exception.SkinRequestException;

/**
 * Created by McLive on 27.08.2019.
 *
 *  @param <P> Platform specific Player
 */
public interface ISkinsRestorerAPI<P> {
    Object getProfile(String uuid);

    String getSkinName(String playerName);
    Object getSkinData(String skinName);

    void setSkin(String playerName, String skinName) throws SkinRequestException;

    void applySkin(P player, Object props);
    void applySkin(P player);
}
