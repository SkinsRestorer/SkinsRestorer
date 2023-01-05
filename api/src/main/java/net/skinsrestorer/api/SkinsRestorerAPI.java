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
import lombok.Getter;
import lombok.NonNull;
import net.skinsrestorer.api.exception.NotPremiumException;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.*;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.SkinProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * API Example: <a href="https://github.com/SkinsRestorer/SkinsRestorerAPIExample">https://github.com/SkinsRestorer/SkinsRestorerAPIExample</a> <br>
 * For more info please refer first to <a href="https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI">https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI</a> <br>
 * Advanced help or getting problems? join our discord before submitting issues!!
 */
@SuppressWarnings({"unused"})
public class SkinsRestorerAPI<P> {
    private static SkinsRestorerAPI<?> api;
    @Getter
    private final IMojangAPI mojangAPI;
    private final IMineSkinAPI mineSkinAPI;
    private final ISkinStorage skinStorage;
    private final SkinApplier<P> skinApplier;
    private final NameGetter<P> nameGetter;
    private final Gson gson = new Gson();
    private Class<P> playerClass;

    public SkinsRestorerAPI(Class<P> playerClass,
                            IMojangAPI mojangAPI,
                            IMineSkinAPI mineSkinAPI,
                            ISkinStorage skinStorage,
                            SkinApplier<P> skinApplier,
                            NameGetter<P> nameGetter) {
        if (SkinsRestorerAPI.api == null) {
            setInstance(this);
        }

        this.mojangAPI = mojangAPI;
        this.mineSkinAPI = mineSkinAPI;
        this.skinStorage = skinStorage;
        this.skinApplier = skinApplier;
        this.nameGetter = nameGetter;
    }

    private static synchronized void setInstance(SkinsRestorerAPI<?> api) {
        if (SkinsRestorerAPI.api == null)
            SkinsRestorerAPI.api = api;
    }

    @SuppressWarnings("unchecked") // We check manually if the class is the same
    public static <T> SkinsRestorerAPI<T> getApi(Class<T> playerClass) {
        if (SkinsRestorerAPI.api == null) {
            throw new IllegalStateException("SkinsRestorerAPI is not initialized yet!");
        }

        if (!SkinsRestorerAPI.api.playerClass.equals(playerClass)) {
            throw new IllegalStateException(String.format("SkinsRestorer API requires %s as player class, but %s was provided!",
                    SkinsRestorerAPI.api.playerClass.getSimpleName(), playerClass.getSimpleName()));
        }

        return (SkinsRestorerAPI<T>) SkinsRestorerAPI.api;
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
    public SkinProperty getSkinData(String skinName) {
        return getSkinStorage().getSkinData(skinName, true).orElse(null);
    }

    /**
     * Set SkinData to SkinsRestorer directly
     *
     * @param skinName  Skin name
     * @param textures  Property object
     * @param timestamp timestamp string in millis (leave null for current)
     * @deprecated use {@link #setSkinData(String, SkinProperty)} or {@link #setSkinData(String, SkinProperty, long)}
     */
    @Deprecated
    public void setSkinData(String skinName, SkinProperty textures, @Nullable Long timestamp) {
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
    public void setSkinData(String skinName, SkinProperty textures) {
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
    public void setSkinData(String skinName, SkinProperty textures, long timestamp) {
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
    public SkinProperty genSkinUrl(String url, @Nullable SkinVariant skinVariant) throws SkinRequestException {
        return mineSkinAPI.genSkin(url, skinVariant);
    }

    /**
     * @param skinName Skin name
     * @return textures.minecraft.net url
     * @see #getSkinTextureUrl(SkinProperty)
     * @deprecated use {@link #getSkinTextureUrl(SkinProperty)} instead
     */
    @Deprecated
    public String getSkinTextureUrl(String skinName) {
        SkinProperty skin = getSkinData(skinName);
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
    public String getSkinTextureUrl(@NonNull SkinProperty property) {
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
     * @see #getSkinTextureUrl(SkinProperty)
     */
    public String getSkinTextureUrlStripped(@NonNull SkinProperty property) {
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
    public MojangProfileResponse getSkinProfileData(@NonNull SkinProperty property) {
        String decodedString = new String(Base64.getDecoder().decode(property.getValue()));

        return gson.fromJson(decodedString, MojangProfileResponse.class);
    }

    public void setSkin(String playerName, String skinName) throws SkinRequestException, NotPremiumException {
        setSkinName(playerName, skinName);
        getSkinStorage().fetchSkinData(skinName);
    }

    /**
     * Removes the player selected skin
     * This will remove the player table/file and NOT the skin data.
     *
     * @param playerName - The players name
     */
    public void removeSkin(String playerName) {
        getSkinStorage().removeSkinOfPlayer(playerName);
    }

    private ISkinStorage getSkinStorage() {
        if (skinStorage.isInitialized()) {
            return skinStorage;
        } else throw new IllegalStateException("SkinStorage is not initialized. Is SkinsRestorer in proxy mode?");
    }

    /**
     * Applies the player selected skin from the player table/file.
     * This is useful in combination with setSkinName.
     *
     * @param player
     * @throws SkinRequestException
     */
    public void applySkin(P player) throws SkinRequestException, NotPremiumException {
        String playerName = nameGetter.getName(player);
        applySkin(player, skinStorage.getSkinNameOfPlayer(playerName).orElse(playerName));
    }

    /**
     * Only Apply the skinName from the skin table/file.
     * This will not keep the skin on rejoin / applySkin(playerWrapper).
     *
     * @param player
     * @param skinName
     * @throws SkinRequestException
     */
    public void applySkin(P player, String skinName) throws SkinRequestException, NotPremiumException {
        applySkin(player, skinStorage.fetchSkinData(skinName));
    }

    /**
     * Applies the skin from the property object.
     * This can be a custom skin that is not in the skin table/file.
     *
     * @param player
     * @param property
     */
    public void applySkin(P player, SkinProperty property) {
        skinApplier.applySkin(player, property);
    }
}
