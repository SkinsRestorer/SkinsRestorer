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
package net.skinsrestorer.api;

import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.event.EventBus;
import net.skinsrestorer.api.property.SkinApplier;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.jetbrains.annotations.ApiStatus;

/**
 * SkinsRestorer API <br>
 * Check out our <a href="https://github.com/SkinsRestorer/SkinsRestorerAPIExample">API example</a> plugin <br>
 * For more info please refer first to the <a href="https://skinsrestorer.net/docs/development/api">SkinsRestorer API Docs</a> <br>
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

    /**
     * Return the version of SkinsRestorer installed on the server.
     *
     * @return The version of SkinsRestorer installed on the server.
     * @see VersionProvider for the official version provider.
     */
    @ApiStatus.Internal
    String getVersion();

    /**
     * Return the commit of SkinsRestorer installed on the server.
     *
     * @return The commit of SkinsRestorer installed on the server.
     * @see VersionProvider for the official version provider.
     */
    @ApiStatus.Internal
    String getCommit();

    /**
     * Return the short commit of SkinsRestorer installed on the server.
     *
     * @return The short commit of SkinsRestorer installed on the server.
     * @see VersionProvider for the official version provider.
     */
    @ApiStatus.Internal
    String getCommitShort();
}
