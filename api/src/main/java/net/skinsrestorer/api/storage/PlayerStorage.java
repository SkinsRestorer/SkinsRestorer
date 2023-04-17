package net.skinsrestorer.api.storage;

import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Optional;
import java.util.UUID;

public interface PlayerStorage {
    /**
     * Get the custom set skin of a player.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the skin that would be set on join
     */
    Optional<SkinIdentifier> getSkinIdOfPlayer(UUID uuid);

    /**
     * Saves players skin identifier to the database
     *
     * @param uuid       Players UUID
     * @param identifier Skin identifier
     */
    void setSkinIdOfPlayer(UUID uuid, SkinIdentifier identifier);

    /**
     * Removes players skin identifier from the database
     *
     * @param uuid Players UUID
     */
    void removeSkinIdOfPlayer(UUID uuid);

    /**
     * Gets the optional set skin identifier of a player and then returns the skin data.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the skin that would be set on join
     */
    Optional<SkinProperty> getSkinOfPlayer(UUID uuid) throws DataRequestException;

    /**
     * Get the skin a player would get if there was no skin set for them.
     *
     * @param uuid Players UUID
     * @return The identifier of the default skin
     * @throws DataRequestException If MojangAPI lookup errors (e.g. mojang offline)
     */
    Optional<SkinProperty> getDefaultSkinForPlayer(UUID uuid, String playerName) throws DataRequestException;

    /**
     * This method seeks out the skin that would be set on join and returns
     * the property containing all the skin data.
     * That skin can either be custom set, the premium skin or a default skin.
     * It also executes a skin data update if the saved skin data expired.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the skin that would be set on join
     * @throws DataRequestException If MojangAPI lookup errors (e.g. mojang offline)
     */
    Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName) throws DataRequestException;
}
