/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownStorage implements Runnable {

    private static final ConcurrentHashMap<String, Long> cooldown = new ConcurrentHashMap<>();

    public static boolean hasCooldown(String name) {
        Long expire = cooldown.get(name);
        return expire != null && expire > System.currentTimeMillis();
    }

    public static void resetCooldown(String name) {
        cooldown.remove(name);
    }

    public static int getCooldown(String name) {
        int int1 = Integer.parseInt(String.format("%d", TimeUnit.MILLISECONDS.toSeconds(cooldown.get(name))));
        int int2 = Integer.parseInt(String.format("%d", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        return int1 - int2;
    }

    public static void setCooldown(String name, int cooldownTime, TimeUnit timeunit) {
        cooldown.put(name, System.currentTimeMillis() + timeunit.toMillis(cooldownTime));
    }

    @Override
    public void run() {
        long current = System.currentTimeMillis();
        CooldownStorage.cooldown.values().removeIf(aLong -> aLong <= current);
    }
}
