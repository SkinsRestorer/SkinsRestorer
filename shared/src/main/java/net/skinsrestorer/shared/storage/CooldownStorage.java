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
package net.skinsrestorer.shared.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownStorage {
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public boolean hasCooldown(UUID uuid) {
        Long expire = cooldown.get(uuid);
        return expire != null && expire > System.currentTimeMillis();
    }

    public int getCooldownSeconds(UUID uuid) {
        return (int) TimeUnit.MILLISECONDS.toSeconds(cooldown.get(uuid) - System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid, int cooldownTime, TimeUnit timeunit) {
        cooldown.put(uuid, System.currentTimeMillis() + timeunit.toMillis(cooldownTime));
    }

    public void cleanup() {
        long current = System.currentTimeMillis();
        cooldown.values().removeIf(expire -> expire <= current);
    }
}
