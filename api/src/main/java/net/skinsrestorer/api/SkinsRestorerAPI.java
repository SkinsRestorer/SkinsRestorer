/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.Getter;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IMineSkinAPI;
import net.skinsrestorer.api.interfaces.IMojangAPI;
import net.skinsrestorer.api.interfaces.ISkinStorage;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * API Example: <a href="https://github.com/SkinsRestorer/SkinsRestorerAPIExample">https://github.com/SkinsRestorer/SkinsRestorerAPIExample</a> <br>
 * For more info please refer first to <a href="https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI">https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI</a> <br>
 * Advanced help or getting problems? join our discord before submitting issues!!
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
        return skinStorage.getSkinName(playerName).orElse(null);
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
        skinStorage.setSkinName(playerName, skinName);
    }

    /**
     * Returns property object containing skin data of the wanted skin
     *
     * @param skinName Skin name
     **/
    public IProperty getSkinData(String skinName) {
        return skinStorage.getSkinData(skinName).orElse(null);
    }

    /**
     * Set SkinData to SkinsRestorer directly
     *
     * @param skinName  Skin name
     * @param textures  Property object
     * @param timestamp timestamp string in millis (leave null for current)
     */
    public void setSkinData(String skinName, IProperty textures, @Nullable Long timestamp) {
        if (timestamp == null) {
            skinStorage.setSkinData(skinName, textures);
        } else {
            skinStorage.setSkinData(skinName, textures, timestamp);
        }
    }

    /**
     * Generates a skin using the https://mineskin.org/ api
     * [WARNING] MineSkin api key might be REQUIRED in the future.
     *
     * @param url      pointing to a skin image url
     * @param skinType can be null, steve or slim
     * @return Custom skin property containing "value" and "signature"
     * @throws SkinRequestException on error
     */
    public IProperty genSkinUrl(String url, @Nullable String skinType) throws SkinRequestException {
        return mineSkinAPI.genSkin(url, skinType, null);
    }

    /**
     * Returns a https://textures.minecraft.net/id based on skin
     * This is Usefull for skull plugins like Dynmap or DiscordSRV
     * for example https://mc-heads.net/avatar/%texture_id%/%size%.png
     *
     * @param skinName
     * @return textures.minecraft.net url
     */
    public String getSkinTextureUrl(String skinName) {
        IProperty skin = getSkinData(skinName);
        if (skin == null)
            return null;
        byte[] decoded = Base64.getDecoder().decode(skin.getValue());
        String decodedString = new String(decoded);
        JsonObject jsonObject = JsonParser.parseString(decodedString).getAsJsonObject();
        String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
        return decodedSkin.substring(1, decodedSkin.length() - 1);
    }

    public void setSkin(String playerName, String skinName) throws SkinRequestException {
        setSkinName(playerName, skinName);
        skinStorage.getSkinForPlayer(skinName);
    }

    public IProperty createProperty(String name, String value, String signature) {
        return mojangAPI.createProperty(name, value, signature);
    }

    public void removeSkin(String playerName) {
        skinStorage.removeSkin(playerName);
    }

    public abstract void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException;

    public abstract void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException;

    public abstract void applySkin(PlayerWrapper playerWrapper, IProperty props);
}
