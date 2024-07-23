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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.codec.NetworkCodec;
import net.skinsrestorer.shared.codec.NetworkId;
import net.skinsrestorer.shared.subjects.messages.Message;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum PageType implements NetworkId {
    SELECT(Message.SKINSMENU_TITLE_SELECT),
    MAIN(Message.SKINSMENU_TITLE_MAIN),
    HISTORY(Message.SKINSMENU_TITLE_HISTORY),
    FAVOURITES(Message.SKINSMENU_TITLE_FAVOURITES);

    public static final NetworkCodec<PageType> CODEC = NetworkCodec.ofEnum(PageType.class);
    private final Message title;

    @Override
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }
}
