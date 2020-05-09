package skinsrestorer.shared.storage.backend;

import skinsrestorer.shared.interfaces.ISkinStorage;

import java.util.Map;

/**
 * Created by McLive on 10.11.2019.
 */
public class SkinStorageMySQL {
    public String getPlayerSkin(String player) {
        return null;
    }

    public Object getSkinData(String name, boolean updateOutdated) {
        return null;
    }

    public void removePlayerSkin(String name) {

    }

    public void removeSkinData(String name) {

    }

    public void setPlayerSkin(String name, String skin) {

    }

    public void setSkinData(String name, Object textures, String timestamp) {

    }

    public Map<String, Object> getSkins(int number) {
        return null;
    }

    public boolean forceUpdateSkinData(String skin) {
        return false;
    }

    public String getDefaultSkinNameIfEnabled(String player, boolean clear) {
        return null;
    }
}
