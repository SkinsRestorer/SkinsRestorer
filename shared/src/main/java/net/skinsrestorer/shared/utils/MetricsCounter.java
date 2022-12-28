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
package net.skinsrestorer.shared.utils;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCounter {
    private final Map<Service, AtomicInteger> map = new EnumMap<>(Service.class);

    public void increment(Service service) {
        getOrCreate(service).incrementAndGet();
    }

    public int collect(Service service) {
        return getOrCreate(service).getAndSet(0);
    }

    private AtomicInteger getOrCreate(Service service) {
        return map.compute(service, (k, v) -> v == null ? new AtomicInteger() : v);
    }

    public int collectMineskinCalls() {
        return collect(Service.MINE_SKIN);
    }

    public int collectMinetoolsCalls() {
        return collect(Service.MINE_TOOLS);
    }

    public int collectMojangCalls() {
        return collect(Service.MOJANG);
    }

    public int collectAshconCalls() {
        return collect(Service.ASHCON);
    }

    public enum Service {
        MINE_SKIN,
        MINE_TOOLS,
        MOJANG,
        ASHCON
    }
}
