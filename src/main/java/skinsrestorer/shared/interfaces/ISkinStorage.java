package skinsrestorer.shared.interfaces;

import java.util.Map;

/**
 * Created by McLive on 10.11.2019.
 */
public interface ISkinStorage {
    String getPlayerSkin(String player);

    Object getSkinData(String name, boolean updateOutdated);
    Object getSkinData(String name);

    void removePlayerSkin(String name);

    void removeSkinData(String name);

    void setPlayerSkin(String name, String skin);

    void setSkinData(String name, Object textures, String timestamp);
    void setSkinData(String name, Object textures);

    Map<String, Object> getSkins(int number);

    boolean forceUpdateSkinData(String skin);

    String getDefaultSkinNameIfEnabled(String player, boolean clear);
    String getDefaultSkinNameIfEnabled(String player);
}
