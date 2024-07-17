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
package net.skinsrestorer.shared.gui;

<<<<<<<< HEAD:shared/src/main/java/net/skinsrestorer/shared/gui/ClickEventType.java
import java.util.Locale;

public enum ClickEventType implements NetworkId {
    LEFT,
    MIDDLE,
    RIGHT,
    OTHER;

    public static final NetworkCodec<ClickEventType> CODEC = CodecHelpers.createEnumCodec(ClickEventType.class);

    @Override
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
========
import lombok.Getter;

import java.util.Locale;
import java.util.Optional;

@Getter
public enum PageType {
    MAIN;

    private final String key = name().toLowerCase(Locale.ROOT);

    public static Optional<PageType> fromKey(String key) {
        for (PageType pageType : values()) {
            if (pageType.key.equals(key)) {
                return Optional.of(pageType);
            }
        }

        return Optional.empty();
>>>>>>>> origin/dev:shared/src/main/java/net/skinsrestorer/shared/gui/PageType.java
    }
}
