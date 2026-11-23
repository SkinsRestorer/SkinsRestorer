/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.shared.connections.mineskin.responses;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.skinsrestorer.shared.connections.mineskin.responses.MineSkinUrlResponse.Skin;
import org.jetbrains.annotations.Nullable;

/**
 * Accepts both the documented top-level SkinInfo lookup response and the deployed v2 envelope.
 *
 * @see <a href="https://github.com/MineSkin/docs/blob/main/spec/openapi.json">Lookup OpenAPI schema</a>
 * @see <a href="https://github.com/MineSkin/api.mineskin.org/blob/main/src/typings/v2/V2SkinResponse.ts">
 * V2 envelope</a>
 */
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class MineSkinLookupResponse extends Skin {
    private Boolean success;
    private Skin skin;

    /** Returns the skin, requiring explicit success when the response uses the v2 envelope. */
    public @Nullable Skin resolveSkin() {
        if (skin != null) {
            return Boolean.TRUE.equals(success) ? skin : null;
        }
        return success == null ? this : null;
    }
}
