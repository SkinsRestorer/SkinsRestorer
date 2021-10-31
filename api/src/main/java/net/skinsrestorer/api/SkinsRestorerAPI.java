/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
 *
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
 */
package net.skinsrestorer.api;

import lombok.AccessLevel;
import lombok.Getter;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IMineSkinAPI;
import net.skinsrestorer.api.interfaces.IMojangAPI;
import net.skinsrestorer.api.interfaces.ISkinStorage;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;

/**
 * API Example: <a href="https://github.com/SkinsRestorer/SkinsRestorerAPIExample">https://github.com/SkinsRestorer/SkinsRestorerAPIExample</a> <br>
 * For more info please refer first to <a href="https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI">https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI</a> <br>
 * Advanced help or getting problems? join our discord before submitting issues!
 */
@SuppressWarnings({"unused"})
public abstract class SkinsRestorerAPI {
    @Getter
    private static SkinsRestorerAPI api;
    private final IMojangAPI mojangAPI;
    private final IMineSkinAPI mineSkinAPI;
    private final ISkinStorage skinStorage;
    @Getter(value = AccessLevel.PROTECTED)
    private final IWrapperFactory wrapperFactory;

    protected SkinsRestorerAPI(IMojangAPI mojangAPI, IMineSkinAPI mineSkinAPI, ISkinStorage skinStorage, IWrapperFactory wrapperFactory) {
        if (SkinsRestorerAPI.api == null)
            setInstance(this);

        this.mojangAPI = mojangAPI;
        this.mineSkinAPI = mineSkinAPI;
        this.skinStorage = skinStorage;
        this.wrapperFactory = wrapperFactory;
    }

    private static void setInstance(SkinsRestorerAPI api) {
        if (SkinsRestorerAPI.api == null)
            SkinsRestorerAPI.api = api;
    }

    /**
     * Returned property contains all skin data.
     * You can get the wrapped object using {@link IProperty#getHandle()}
     *
     * @param uuid The players uuid
     * @return The players skin property
     **/
    public IProperty getProfile(String uuid) {
        return mojangAPI.getProfile(uuid).orElse(null);
    }

    /**
     * Returns the custom skin name that player has set.
     * Returns null if player has no custom skin set.
     *
     * @param playerName The players name
     * @return The players custom skin name if set or null if not set
     */
    public String getSkinName(String playerName) {
        return skinStorage.getSkinOfPlayer(playerName).orElse(null);
    }

    /**
     * Check if a player got a custom skin.
     *
     * @param playerName The players name
     * @return true if a player has a custom skin set
     */
    public boolean hasSkin(String playerName) {
        return getSkinName(playerName) != null;
    }

    /**
     * Saves custom player's skin name to database
     *
     * @param playerName Players name
     * @param skinName   Skin name
     **/
    public void setSkinName(String playerName, String skinName) {
        skinStorage.setSkinNameOfPlayer(playerName, skinName);
    }

    /**
     * Returns property object containing skin data of the wanted skin
     *
     * @param skinName Skin name
     **/
    public IProperty getSkinData(String skinName) {
        return skinStorage.getSkinData(skinName).orElse(null);
    }

    public void setSkin(String playerName, String skinName) throws SkinRequestException {
        setSkinName(playerName, skinName);
        skinStorage.getSkinForPlayer(skinName);
    }

    public void removeSkin(String playerName) {
        skinStorage.removeSkinOfPlayer(playerName);
    }

    public abstract void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException;

    public abstract void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException;

    public abstract void applySkin(PlayerWrapper playerWrapper, IProperty props);
}
