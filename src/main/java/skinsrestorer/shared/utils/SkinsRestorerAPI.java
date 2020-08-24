package skinsrestorer.shared.utils;

import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.SkinStorage;

/**
 * Created by McLive on 27.08.2019.
 */

/* APIexample: https://github.com/SkinsRestorer/SkinsRestorerAPIExample
   For more info please refer first to https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI
   Advanced help or getting problems? join our discord before submitting issues!

   [Warning!] Make sure to use SkinsRestorerBukkitAPI.java to apply skin. */

public class SkinsRestorerAPI {
    private MojangAPI mojangAPI;
    private SkinStorage skinStorage;

    public SkinsRestorerAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
        this.mojangAPI = mojangAPI;
        this.skinStorage = skinStorage;
    }

    public String getUUID(String playerName) throws SkinRequestException {
        return mojangAPI.getUUID(playerName);
    }

    public Object getProfile(String uuid) {
        return mojangAPI.getSkinProperty(uuid);
    }

    public String getSkinName(String playerName) {
        return skinStorage.getPlayerSkin(playerName);
    }

    public Object getSkinData(String skinName) {
        return skinStorage.getSkinData(skinName);
    }

    public boolean hasSkin(String playerName) {
        return skinStorage.getPlayerSkin(playerName) != null;
    }

    public void setSkinName(String playerName, String skinName) {
        skinStorage.setPlayerSkin(playerName, skinName);
    }

    public void setSkin(String playerName, String skinName) throws SkinRequestException {
        skinStorage.setPlayerSkin(playerName, skinName);
        skinStorage.getOrCreateSkinForPlayer(skinName);
    }

    public void removeSkin(String playername) {
        skinStorage.removePlayerSkin(playername);
    }
}
