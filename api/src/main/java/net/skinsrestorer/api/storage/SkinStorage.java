/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.api.storage;

import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SkinStorage
 * <p>
 * Skin name: A name assigned to a skin property. Cached in a .skin file with a timestamp for expiry.
 * Player skin: Stored as a skin name in a .player file.
 */
public interface SkinStorage {
    /**
     * This method returns the skin data associated to the skin name.
     * If the skin name is not found, it will try to get the skin data from Mojang.
     *
     * @param uuid Player UUID
     * @return The skin property containing the skin data
     * @throws DataRequestException If MojangAPI lookup errors (e.g. premium player not found)
     */
    Optional<SkinProperty> updatePlayerSkinData(UUID uuid) throws DataRequestException;

    /**
     * Saves skin data to database
     *
     * @param uuid      Player UUID
     * @param textures  Property object
     * @param timestamp timestamp string in milliseconds
     */
    void setPlayerSkinData(UUID uuid, SkinProperty textures, long timestamp);

    /**
     * Saves skin data to database
     *
     * @param url         URL to skin
     * @param mineSkinId  MineSkin ID
     * @param textures    Property object
     * @param skinVariant Skin variant
     */
    void setURLSkinData(String url, String mineSkinId, SkinProperty textures, SkinVariant skinVariant);

    /**
     * Saves skin data to database
     *
     * @param url         URL to skin
     * @param skinVariant Skin variant
     */
    void setURLSkinIndex(String url, SkinVariant skinVariant);

    default void setURLSkinByResponse(String url, MineSkinResponse response) {
        if (response.getRequestedVariant() == null) {
            setURLSkinIndex(url, response.getGeneratedVariant());
        }

        setURLSkinData(url, response.getMineSkinId(), response.getProperty(), response.getGeneratedVariant());
    }

    /**
     * Saves skin data to database
     *
     * @param skinName Skin name
     * @param textures Property object
     */
    void setCustomSkinData(String skinName, SkinProperty textures);

    Optional<InputDataResult> findSkinData(String input);

    Optional<InputDataResult> findOrCreateSkinData(String input) throws DataRequestException;

    Optional<SkinProperty> getSkinDataByIdentifier(SkinIdentifier identifier);

    void removeSkinData(SkinIdentifier identifier);

    boolean purgeOldSkins(int days);

    Map<String, String> getGUISkins(int skinNumber);
}
