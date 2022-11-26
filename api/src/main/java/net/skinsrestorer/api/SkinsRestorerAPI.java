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

import com.google.gson.Gson;
import lombok.NonNull;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.*;
import net.skinsrestorer.api.model.MojangProfileResponse;
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
    private static SkinsRestorerAPI api;
    private final IMojangAPI mojangAPI;
    private final IMineSkinAPI mineSkinAPI;
    private final ISkinStorage skinStorage;
    private final IPropertyFactory propertyFactory;
    private final Gson gson = new Gson();
    private final IWrapperFactory wrapperFactory;

    protected SkinsRestorerAPI(IMojangAPI mojangAPI, IMineSkinAPI mineSkinAPI, ISkinStorage skinStorage, IWrapperFactory wrapperFactory, IPropertyFactory propertyFactory) {
        if (SkinsRestorerAPI.api == null)
            setInstance(this);

        this.mojangAPI = mojangAPI;
        this.mineSkinAPI = mineSkinAPI;
        this.skinStorage = skinStorage;
        this.wrapperFactory = wrapperFactory;
        this.propertyFactory = propertyFactory;
    }

    private static synchronized void setInstance(SkinsRestorerAPI api) {
        if (SkinsRestorerAPI.api == null)
            SkinsRestorerAPI.api = api;
    }

    public static SkinsRestorerAPI getApi() {
        if (SkinsRestorerAPI.api == null)
            throw new IllegalStateException("SkinsRestorerAPI is not initialized yet!");

        return SkinsRestorerAPI.api;
    }

    @SuppressWarnings("JavadocReference")
    /**
     *  Get the trimmed uuid from a player playerName
     *
     * @param playerName Mojang username of the player
     * @return String uuid trimmed (without dashes)
     * @throws NotPremiumException if the player is not premium
     * @throws SkinRequestException or error
     */

    public String getUUID(@NonNull String playerName) throws SkinRequestException {
        return this.mojangAPI.getUUID(playerName);
    }

    /**
     * Returned property contains all skin data.
     * You can get the wrapped object using {@link IProperty#getHandle()}
     *
     * @param uuid The players uuid
     * @return The players skin property, null if not found
     **/
    @Nullable
    public IProperty getProfile(@NonNull String uuid) {
        return mojangAPI.getProfile(uuid).orElse(null);
    }

    /**
     * Returns the custom skin name that player has set.
     * Returns null if player has no custom skin set.
     *
     * @param playerName The players name
     * @return The players custom skin name, or null if player has no skin set.
     */
    @Nullable
    public String getSkinName(String playerName) {
        return getSkinStorage().getSkinNameOfPlayer(playerName).orElse(null);
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
        getSkinStorage().setSkinOfPlayer(playerName, skinName);
    }

    /**
     * Returns property object containing skin data of the wanted skin
     *
     * @param skinName Skin name
     * @return Property object containing skin data, or null if skin not found.
     **/
    @Nullable
    public IProperty getSkinData(String skinName) {
        return getSkinStorage().getSkinData(skinName, true).orElse(null);
    }

    /**
     * Set SkinData to SkinsRestorer directly
     *
     * @param skinName  Skin name
     * @param textures  Property object
     * @param timestamp timestamp string in millis (leave null for current)
     * @deprecated use {@link #setSkinData(String, IProperty)} or {@link #setSkinData(String, IProperty, long)}
     */
    @Deprecated
    public void setSkinData(String skinName, IProperty textures, @Nullable Long timestamp) {
        if (timestamp == null) {
            setSkinData(skinName, textures);
        } else {
            setSkinData(skinName, textures, (long) timestamp);
        }
    }

    /**
     * Set stored properties of a skin in storage.
     * Only changes stored data, does not refresh anyone who has the skin.
     *
     * @param skinName Skin name
     * @param textures Property object
     */
    public void setSkinData(String skinName, IProperty textures) {
        getSkinStorage().setSkinData(skinName, textures);
    }

    /**
     * Set stored properties of a skin in storage.
     * Only changes stored data, does not refresh anyone who has the skin.
     *
     * @param skinName  Skin name
     * @param textures  Property object
     * @param timestamp timestamp long in millis
     */
    public void setSkinData(String skinName, IProperty textures, long timestamp) {
        getSkinStorage().setSkinData(skinName, textures, timestamp);
    }

    /**
     * Generates a skin using the <a href="https://mineskin.org/">MineSkin</a> API
     * [WARNING] MineSkin api key might be REQUIRED in the future.
     *
     * @param url         pointing to a skin image url
     * @param skinVariant can be null, steve or slim
     * @return Custom skin property containing "value" and "signature"
     * @throws SkinRequestException on error
     */
    public IProperty genSkinUrl(String url, @Nullable SkinVariant skinVariant) throws SkinRequestException {
        return mineSkinAPI.genSkin(url, skinVariant);
    }

    /**
     * @param skinName Skin name
     * @return textures.minecraft.net url
     * @see #getSkinTextureUrl(IProperty)
     * @deprecated use {@link #getSkinTextureUrl(IProperty)} instead
     */
    @Deprecated
    public String getSkinTextureUrl(String skinName) {
        IProperty skin = getSkinData(skinName);
        if (skin == null)
            return null;

        return getSkinTextureUrl(skin);
    }

    /**
     * Returns a <a href="https://textures.minecraft.net/id">Texture Url</a> based on skin
     * This is useful for skull plugins like Dynmap or DiscordSRV
     * for example <a href="https://mc-heads.net/avatar/cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f/100.png">https://mc-heads.net/avatar/%texture_id%/%size%.png</a>
     *
     * @param property Profile property
     * @return full textures.minecraft.net url
     */
    public String getSkinTextureUrl(@NonNull IProperty property) {
        return getSkinProfileData(property).getTextures().getSKIN().getUrl();
    }

    /**
     * Only returns the id at the end of the url.
     * Example:
     * <a href="https://textures.minecraft.net/texture/cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f">
     * https://textures.minecraft.net/texture/cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f
     * </a>
     * Would return: cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f
     *
     * @param property Profile property
     * @return textures.minecraft.net id
     * @see #getSkinTextureUrl(IProperty)
     */
    public String getSkinTextureUrlStripped(@NonNull IProperty property) {
        return getSkinProfileData(property).getTextures().getSKIN().getStrippedUrl();
    }

    /**
     * Returns the decoded profile data from the profile property.
     * This is useful for getting the skin data from the property and other information like cape.
     * The user stored in this property may not be the same as the player who has the skin.
     * APIs like MineSkin use multiple shared accounts to generate these properties.
     * Or it could be the property of another player that the player set their skin to.
     *
     * @param property Profile property
     * @return Decoded profile data as java object
     */
    public MojangProfileResponse getSkinProfileData(@NonNull IProperty property) {
        String decodedString = new String(Base64.getDecoder().decode(property.getValue()));

        return gson.fromJson(decodedString, MojangProfileResponse.class);
    }

    public void setSkin(String playerName, String skinName) throws SkinRequestException {
        setSkinName(playerName, skinName);
        getSkinStorage().fetchSkinData(skinName);
    }

    public IProperty createPlatformProperty(IProperty property) {
        return createPlatformProperty(property.getName(), property.getValue(), property.getSignature());
    }

    public IProperty createPlatformProperty(String name, String value, String signature) {
        return propertyFactory.createProperty(name, value, signature);
    }

    /**
     * @see #createPlatformProperty(String, String, String)
     */
    @Deprecated
    public IProperty createProperty(String name, String value, String signature) {
        return createPlatformProperty(name, value, signature);
    }

    public void removeSkin(String playerName) {
        getSkinStorage().removeSkinOfPlayer(playerName);
    }

    private ISkinStorage getSkinStorage() {
        if (skinStorage.isInitialized()) {
            return skinStorage;
        } else throw new IllegalStateException("SkinStorage is not initialized. Is SkinsRestorer in proxy mode?");
    }

    public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
        applySkin(playerWrapper, playerWrapper.getName());
    }

    public void applySkin(PlayerWrapper playerWrapper, String skinName) throws SkinRequestException {
        applySkin(playerWrapper, skinStorage.fetchSkinData(skinName));
    }

    public abstract void applySkin(PlayerWrapper playerWrapper, IProperty property);

    protected IWrapperFactory getWrapperFactory() {
        return this.wrapperFactory;
    }
}
