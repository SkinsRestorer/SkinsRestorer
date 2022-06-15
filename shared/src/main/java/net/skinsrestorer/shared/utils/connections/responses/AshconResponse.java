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
package net.skinsrestorer.shared.utils.connections.responses;

import lombok.Getter;
import net.skinsrestorer.api.property.IProperty;

@Getter
public class AshconResponse {
    /**
     * UUID with dashes
     */
    private String uuid;
    private String name;
    private Textures textures;
    private int code;
    private String error;
    private String reason;

    @Getter
    public static class Textures {
        private Raw raw;

        @Getter
        public static class Raw implements IProperty {
            private String value;
            private String signature;

            @Override
            public String getName() {
                return IProperty.TEXTURE_KEY;
            }
        }
    }
}
