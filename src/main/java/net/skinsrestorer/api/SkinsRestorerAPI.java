/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.skinsrestorer.api;

import lombok.Getter;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.MojangAPI;

/**
 * API Example: https://github.com/SkinsRestorer/SkinsRestorerAPIExample
 * For more info please refer first to https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI
 * Advanced help or getting problems? join our discord before submitting issues!
 */
@SuppressWarnings({"unused"})
public abstract class SkinsRestorerAPI {
    @Getter
    private static SkinsRestorerAPI api;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;

    protected SkinsRestorerAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
        if (api == null)
          setInstance(this);

        this.mojangAPI = mojangAPI;
        this.skinStorage = skinStorage;
    }

    private static void setInstance(SkinsRestorerAPI api) {
        SkinsRestorerAPI.api = api;
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
        skinStorage.getOrCreateSkinForPlayer(skinName, false);
    }

    public void removeSkin(String playerName) {
        skinStorage.removePlayerSkin(playerName);
    }

    public abstract void applySkin(PlayerWrapper player, Object props);

    public abstract void applySkin(PlayerWrapper player);
}
