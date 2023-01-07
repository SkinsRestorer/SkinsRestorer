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
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.exception.NotPremiumException;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.SkinStorage;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.util.Pair;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.SkinRequestExceptionShared;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinStorageImpl implements SkinStorage {
    private final SRLogger logger;
    private final MojangAPIImpl mojangAPI;
    private final MineSkinAPIImpl mineSkinAPI;
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    @Setter
    private StorageAdapter storageAdapter;

    public void preloadDefaultSkins() {
        if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        List<String> defaultSkins = new ArrayList<>(settings.getProperty(StorageConfig.DEFAULT_SKINS));
        defaultSkins.forEach(skin -> {
            // TODO: add try for skinUrl
            try {
                if (!C.validUrl(skin)) {
                    fetchSkinData(skin);
                }
            } catch (SkinRequestException | NotPremiumException e) {
                // removing skin from list
                toRemove.add(skin);
                logger.warning("[WARNING] DefaultSkin '" + skin + "'(.skin) could not be found or requested! Removing from list..");

                logger.debug("[DEBUG] DefaultSkin '" + skin + "' error: ", e);
            }
        });
        if (!toRemove.isEmpty()) {
            defaultSkins.removeAll(toRemove);
            settings.setProperty(StorageConfig.DEFAULT_SKINS, defaultSkins);
        }

        if (defaultSkins.isEmpty()) {
            logger.warning("[WARNING] No more working DefaultSkin left... disabling feature");
            settings.setProperty(StorageConfig.DEFAULT_SKINS_ENABLED, false);
        }
    }

    @Override
    public Pair<SkinProperty, Boolean> getDefaultSkinForPlayer(String playerName) throws SkinRequestException, NotPremiumException {
        Pair<String, Boolean> result = getDefaultSkinName(playerName, false);
        String skin = result.getLeft();

        if (C.validUrl(skin)) {
            return Pair.of(mineSkinAPI.genSkin(skin, null), result.getRight());
        } else {
            return Pair.of(fetchSkinData(skin), result.getRight());
        }
    }

    @Override
    public SkinProperty fetchSkinData(String skinName) throws SkinRequestException, NotPremiumException {
        Optional<SkinProperty> textures = getSkinData(skinName, true);
        if (!textures.isPresent()) {
            // No cached skin found, get from MojangAPI, save and return
            try {
                textures = mojangAPI.getSkin(skinName);

                if (!textures.isPresent()) {
                    throw new SkinRequestExceptionShared(locale, Message.ERROR_NO_SKIN);
                }

                setSkinData(skinName, textures.get());

                return textures.get();
            } catch (SkinRequestException | NotPremiumException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();

                throw new SkinRequestExceptionShared(locale, Message.WAIT_A_MINUTE);
            }
        } else {
            return textures.get();
        }
    }

    @Override
    public Optional<String> getSkinNameOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        Optional<String> optional = storageAdapter.getStoredSkinNameOfPlayer(playerName);

        return optional.isPresent() && !optional.get().isEmpty() ? optional : Optional.empty();
    }

    /**
     * Create a platform specific property and also optionally update cached skin if outdated.
     *
     * @param playerName     the players name
     * @param updateOutdated whether the skin data shall be looked up again if the timestamp is too far away
     * @param value          skin data value
     * @param signature      signature to verify skin data
     * @param timestamp      time cached property data was created
     * @return Platform-specific property
     * @throws NotPremiumException throws when no API calls were successful
     */
    private SkinProperty createProperty(String playerName, boolean updateOutdated, String value, String signature, long timestamp) throws NotPremiumException {
        if (updateOutdated && C.validMojangUsername(playerName) && isExpired(timestamp)) {
            Optional<SkinProperty> skin = mojangAPI.getSkin(playerName);

            if (skin.isPresent()) {
                setSkinData(playerName, skin.get());
                return skin.get();
            }
        }

        return SkinProperty.of(value, signature);
    }

    @Override
    public void removeSkinOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        storageAdapter.removeStoredSkinNameOfPlayer(playerName);
    }

    @Override
    public void setSkinOfPlayer(String playerName, String skinName) {
        playerName = playerName.toLowerCase();

        storageAdapter.setStoredSkinNameOfPlayer(playerName, skinName);
    }

    // #getSkinData() also create while we have #getSkinForPlayer()
    @Override
    public Optional<SkinProperty> getSkinData(String skinName, boolean updateOutdated) {
        skinName = skinName.toLowerCase();

        try {
            Optional<StorageAdapter.StoredProperty> property = storageAdapter.getStoredSkinData(skinName);

            if (!property.isPresent()) {
                return Optional.empty();
            }

            return Optional.of(createProperty(skinName, updateOutdated, property.get().getValue(), property.get().getSignature(), property.get().getTimestamp()));
        } catch (StorageAdapter.StorageException | NotPremiumException e) {
            logger.info(String.format("Unsupported skin format.. removing (%s).", skinName));
            removeSkinData(skinName);
            return Optional.empty();
        }
    }

    /**
     * Removes skin data from database
     *
     * @param skinName Skin name
     */
    public void removeSkinData(String skinName) {
        skinName = skinName.toLowerCase();

        storageAdapter.removeStoredSkinData(skinName);
    }

    @Override
    public void setSkinData(String skinName, SkinProperty textures) {
        setSkinData(skinName, textures, System.currentTimeMillis());
    }

    @Override
    public void setSkinData(String skinName, SkinProperty textures, long timestamp) {
        skinName = skinName.toLowerCase();
        String value = textures.getValue();
        String signature = textures.getSignature();

        if (value.isEmpty() || signature.isEmpty())
            return;

        storageAdapter.setStoredSkinData(skinName, new StorageAdapter.StoredProperty(value, signature, timestamp));
    }

    // TODO: CUSTOM_GUI
    // seems to be that crs order is ignored...
    public Map<String, String> getSkins(int offset) {
        return storageAdapter.getStoredSkins(offset);
    }

    /**
     * @param skinName Skin name
     * @return true on updated
     * @throws SkinRequestException On updating disabled OR invalid username + api error
     */
    // skin update [include custom skin flag]
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean updateSkinData(String skinName) throws SkinRequestException {
        if (!C.validMojangUsername(skinName))
            throw new SkinRequestExceptionShared(locale, Message.ERROR_UPDATING_CUSTOMSKIN);

        // Check if updating is disabled for skin (by timestamp = 0)
        boolean updateDisabled = storageAdapter.getStoredTimestamp(skinName).map(timestamp -> timestamp == 0).orElse(false);

        if (updateDisabled)
            throw new SkinRequestExceptionShared(locale, Message.ERROR_UPDATING_CUSTOMSKIN);

        // Update Skin
        try {
            Optional<String> mojangUUID = mojangAPI.getUUIDMojang(skinName);

            if (mojangUUID.isPresent()) {
                Optional<SkinProperty> textures = mojangAPI.getProfileMojang(mojangUUID.get());

                if (textures.isPresent()) {
                    setSkinData(skinName, textures.get());
                    return true;
                }
            }
        } catch (NotPremiumException e) {
            throw new SkinRequestExceptionShared(locale, Message.ERROR_UPDATING_CUSTOMSKIN);
        }

        return false;
    }

    /**
     * Filters player name to exclude non [a-z_]
     * Checks and process default skin.
     * IF no default skin:
     * 1: Return player if clear
     * 2: Return skin if found
     * Else: return player
     *
     * @param playerName Player name
     * @param clear      ignore custom set skin of player
     * @return Custom skin or default skin or player name, right side indicates if it is a custom skin
     */
    public Pair<String, Boolean> getDefaultSkinName(String playerName, boolean clear) {
        // Trim player name
        playerName = playerName.trim();

        if (!clear) {
            Optional<String> playerSkinName = getSkinNameOfPlayer(playerName);

            if (playerSkinName.isPresent()) {
                return Pair.of(playerSkinName.get(), true);
            }
        }

        if (settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            // don't return default skin name for premium players if enabled
            if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_PREMIUM)) {
                // check if player is premium
                try {
                    if (mojangAPI.getUUID(playerName).isPresent()) {
                        // player is premium, return his skin name instead of default skin
                        return Pair.of(playerName, false);
                    }
                } catch (NotPremiumException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set, or we want to clear to default
            List<String> skins = settings.getProperty(StorageConfig.DEFAULT_SKINS);

            // return player name if there are no default skins set
            if (skins.isEmpty())
                return Pair.of(playerName, false);

            // makes no sense to select a random skin if there is only one
            if (skins.size() == 1) {
                return Pair.of(skins.get(0), false);
            }

            return Pair.of(skins.get(ThreadLocalRandom.current().nextInt(skins.size())), false);
        }

        // empty if player has no custom skin, we'll return his name then
        return Pair.of(playerName, false);
    }

    /**
     * Checks if updating skins is disabled and if skin expired
     *
     * @param timestamp in milliseconds
     * @return true if skin is outdated
     */
    private boolean isExpired(long timestamp) {
        // Don't update if timestamp is not 0 or update is disabled.
        if (timestamp == 0 || settings.getProperty(StorageConfig.DISALLOW_AUTO_UPDATE_SKIN))
            return false;

        return timestamp + TimeUnit.MINUTES.toMillis(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER)) <= System.currentTimeMillis();
    }

    public boolean purgeOldSkins(int days) {
        long targetPurgeTimestamp = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli();

        try {
            storageAdapter.purgeStoredOldSkins(targetPurgeTimestamp);
            return true; // TODO: Do better than true/false return
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
            return false;
        }
    }
}
