/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
package net.skinsrestorer.api;

import net.skinsrestorer.shared.interfaces.ISRPlayer;

/**
 * Makes it possible to get all platforms into a single API merged.
 */
public class PlayerWrapper {
    private final Object playerInstance;
    private final ISRPlayer internalPlayer;

    public PlayerWrapper(Object playerInstance) {
        this.playerInstance = playerInstance;

        this.internalPlayer = SkinsRestorerAPI.getApi().getWrapperFactory().wrap(playerInstance);
    }

    public <A> A get(Class<A> playerClass) {
        return playerClass.cast(playerInstance);
    }

    public String getName() {
        return internalPlayer.getName();
    }

    public void sendMessage(String message) {
        internalPlayer.sendMessage(message);
    }
}
