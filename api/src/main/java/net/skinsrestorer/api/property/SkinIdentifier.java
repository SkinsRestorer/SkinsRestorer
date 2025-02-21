/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.api.property;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * A skin identifier represents a reference skin that can be applied to a player.
 * A skin identifier always identifies a *stored* skin inside the storage.
 * (eg. file or database)
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SkinIdentifier {
    @NonNull
    private final String identifier;
    /**
     * Only used for {@link SkinType#URL}, otherwise null.
     */
    private final @Nullable SkinVariant skinVariant;
    @NonNull
    private final SkinType skinType;

    /**
     * Create a new SkinIdentifier for a player skin in the storage.
     * A player is identified by his UUID that was assigned to them by Mojang.
     * This is for getting a public mojang skin, not the skin of a player that is on the server.
     * For getting the skin identifier of a player on the server,
     * see {@link PlayerStorage#getSkinIdOfPlayer(UUID)}.
     *
     * @param uuid The UUID of the premium player.
     * @return A new SkinIdentifier.
     */
    public static SkinIdentifier ofPlayer(UUID uuid) {
        return new SkinIdentifier(uuid.toString(), null, SkinType.PLAYER);
    }

    /**
     * Create a new SkinIdentifier for a web URL.
     * The url must resolve to a valid skin png file.
     * The variant identifies a specific skin variant of the skin file.
     * If null, the plugin will use what MineSkin determines as the default variant.
     *
     * @param url         The URL of the skin.
     * @param skinVariant The variant of the skin.
     * @return A new SkinIdentifier.
     */
    public static SkinIdentifier ofURL(String url, @Nullable SkinVariant skinVariant) {
        return new SkinIdentifier(url, skinVariant, SkinType.URL);
    }

    /**
     * Create a new SkinIdentifier for a custom skin.
     * A custom skin can either be hardcoded into the plugin or be created by an admin.
     * In a command, it's checked first if the skin is a custom skin, then if it's a player skin.
     *
     * @param skinName The name of the custom skin.
     * @return A new SkinIdentifier.
     */
    public static SkinIdentifier ofCustom(String skinName) {
        return new SkinIdentifier(skinName.toLowerCase(Locale.ROOT), null, SkinType.CUSTOM);
    }

    /**
     * Not recommended to use, use the other methods instead.
     * Only use is for storage.
     *
     * @param skinIdentifier The identifier can be a UUID, a URL or a custom name.
     * @param skinVariant    Only used for {@link SkinType#URL}, otherwise null.
     * @param skinType       The type of the skin.
     * @return A new SkinIdentifier.
     */
    @ApiStatus.Internal
    public static SkinIdentifier of(String skinIdentifier, @Nullable SkinVariant skinVariant, SkinType skinType) {
        return new SkinIdentifier(skinIdentifier, skinVariant, skinType);
    }

    /**
     * Converts the {@link String} identifier to a {@link UUID}.
     * This is only possible if the skin type is {@link SkinType#PLAYER}.
     *
     * @return The UUID of the player.
     * @throws IllegalStateException If the skin type is not {@link SkinType#PLAYER}.
     */
    public UUID getPlayerUniqueId() {
        if (skinType != SkinType.PLAYER) {
            throw new IllegalStateException("This skin identifier is not for a player skin.");
        }

        return UUID.fromString(identifier);
    }
}
