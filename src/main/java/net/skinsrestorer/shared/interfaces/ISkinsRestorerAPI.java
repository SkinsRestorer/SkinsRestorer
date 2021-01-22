package net.skinsrestorer.shared.interfaces;

import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.utils.PlayerWrapper;

/**
 * Created by McLive on 27.08.2019.
 *
 *  @param <P> Platform specific Player
 */
public interface ISkinsRestorerAPI<P> {
    Object getProfile(String uuid);

    String getSkinName(String playerName);

    void setSkin(String playerName, String skinName) throws SkinRequestException;

    void applySkin(PlayerWrapper player, Object props);
    void applySkin(PlayerWrapper player);
}
