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
package net.skinsrestorer.shared.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ExpiringSet<E> {
    private final Map<E, Long> cache = new HashMap<>();
    private final long lifetime;

    public ExpiringSet(long duration, TimeUnit unit) {
        this.lifetime = unit.toSeconds(duration);
    }

    public void add(E item) {
        cleanup();
        this.cache.put(item, SRHelpers.getEpochSecond() + this.lifetime);
    }

    public boolean contains(E item) {
        cleanup();
        return this.cache.containsKey(item);
    }

    public void cleanup() {
        this.cache.entrySet().removeIf(entry -> entry.getValue() < SRHelpers.getEpochSecond());
    }
}
