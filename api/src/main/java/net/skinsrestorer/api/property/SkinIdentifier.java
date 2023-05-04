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
package net.skinsrestorer.api.property;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.model.SkinVariant;

import java.util.UUID;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SkinIdentifier {
    @NonNull
    private final String identifier;
    /**
     * Only used for {@link SkinType#URL}, otherwise null.
     */
    private final SkinVariant skinVariant;
    @NonNull
    private final SkinType skinType;

    public static SkinIdentifier ofPlayer(UUID uuid) {
        return new SkinIdentifier(uuid.toString(), null, SkinType.PLAYER);
    }

    public static SkinIdentifier ofURL(String url, SkinVariant skinVariant) {
        return new SkinIdentifier(url, skinVariant, SkinType.URL);
    }

    public static SkinIdentifier ofCustom(String skinName) {
        return new SkinIdentifier(skinName, null, SkinType.CUSTOM);
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
    public static SkinIdentifier of(String skinIdentifier, SkinVariant skinVariant, SkinType skinType) {
        return new SkinIdentifier(skinIdentifier, skinVariant, skinType);
    }
}
