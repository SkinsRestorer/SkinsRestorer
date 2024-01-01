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
package net.skinsrestorer.shared.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.event.EventBus;
import net.skinsrestorer.api.property.SkinApplier;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.api.event.EventBusImpl;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedSkinsRestorer implements SkinsRestorer {
    @Getter
    private final SkinStorage skinStorage;
    @Getter
    private final PlayerStorage playerStorage;
    @Getter
    private final CacheStorage cacheStorage;
    @Getter
    private final MojangAPI mojangAPI;
    @Getter
    private final MineSkinAPI mineSkinAPI;
    private final SharedSkinApplier<?> skinApplier;
    private final EventBusImpl eventBus;

    @SuppressWarnings("unchecked")
    @Override
    public <P> SkinApplier<P> getSkinApplier(Class<P> playerClass) {
        if (!skinApplier.accepts(playerClass)) {
            throw new IllegalArgumentException("Unsupported player class: " + playerClass.getName());
        }

        return (SkinApplier<P>) skinApplier;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public String getVersion() {
        return BuildData.VERSION;
    }

    @Override
    public String getCommit() {
        return BuildData.COMMIT;
    }

    @Override
    public String getCommitShort() {
        return BuildData.COMMIT_SHORT;
    }
}
