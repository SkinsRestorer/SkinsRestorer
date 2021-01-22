package net.skinsrestorer.shared.interfaces;

import java.util.Map;

/**
 * Created by McLive on 10.11.2019.
 */
public interface ISkinStorage {
    String getPlayerSkin(String player);

    Object getSkinData(String name, boolean updateOutdated);
    Object getSkinData(String name);

    void removePlayerSkin(String name);

    void setPlayerSkin(String name, String skin);

    Map<String, Object> getSkins(int number);

    String getDefaultSkinNameIfEnabled(String player, boolean clear);
    String getDefaultSkinNameIfEnabled(String player);
}
