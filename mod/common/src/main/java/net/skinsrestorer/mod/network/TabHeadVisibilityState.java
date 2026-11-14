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
package net.skinsrestorer.mod.network;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether the server we're currently connected to told us (via
 * {@link TabHeadVisibilityPayload}) that it already shows a head next to our name in
 * the tab list itself, so {@code PlayerTabOverlayMixin} should not force the vanilla
 * icon on top of it.
 * <p>
 * Reset to {@code false} on disconnect so a later connection to a plain vanilla/other
 * server doesn't keep suppressing the icon based on a previous server's setting.
 */
public final class TabHeadVisibilityState {
    private static final AtomicBoolean SUPPRESSED = new AtomicBoolean(false);

    public static void set(boolean suppressed) {
        SUPPRESSED.set(suppressed);
    }

    public static boolean isSuppressed() {
        return SUPPRESSED.get();
    }

    private TabHeadVisibilityState() {
    }
}
