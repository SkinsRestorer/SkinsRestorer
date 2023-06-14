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
package net.skinsrestorer.api;

import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.event.EventBus;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;

/**
 * SkinsRestorer API <br>
 * API Example: <a href="https://github.com/SkinsRestorer/SkinsRestorerAPIExample">https://github.com/SkinsRestorer/SkinsRestorerAPIExample</a> <br>
 * For more info please refer first to <a href="https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI">https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI</a> <br>
 * Advanced help or getting problems? join our discord before submitting issues!!
 */
@SuppressWarnings({"unused"})
public interface SkinsRestorer {
    /**
     * @return SkinStorage instance
     * @see SkinStorage for more info
     */
    SkinStorage getSkinStorage();

    /**
     * @return PlayerStorage instance
     * @see PlayerStorage for more info
     */
    PlayerStorage getPlayerStorage();

    /**
     * @return CacheStorage instance
     * @see CacheStorage for more info
     */
    CacheStorage getCacheStorage();

    /**
     * @return MojangAPI instance
     * @see MojangAPI for more info
     */
    MojangAPI getMojangAPI();

    /**
     * @return MineSkinAPI instance
     * @see MineSkinAPI for more info
     */
    MineSkinAPI getMineSkinAPI();

    /**
     * @param playerClass class of the player class in your server implementation
     * @param <P>         player class
     * @return SkinApplier instance
     * @see SkinApplier for more info
     */
    <P> SkinApplier<P> getSkinApplier(Class<P> playerClass);

    /**
     * @return EventBus instance
     * @see EventBus for more info
     */
    EventBus getEventBus();
}
