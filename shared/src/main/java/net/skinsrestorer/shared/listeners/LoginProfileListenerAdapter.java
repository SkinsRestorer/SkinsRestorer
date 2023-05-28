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
package net.skinsrestorer.shared.listeners;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.config.LoginConfig;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AtomicAdapter;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;

import javax.inject.Inject;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class LoginProfileListenerAdapter<R> {
    private final SettingsManager settings;
    private final PlayerStorage playerStorage;
    private final SRLogger logger;
    private final AtomicAdapter atomicAdapter;

    public R handleLogin(SRLoginProfileEvent<R> event) {
        logger.debug("Handling login for " + event.getPlayerName() + " (" + event.getPlayerUniqueId() + ")");
        if (handleSync(event)) {
            return null;
        }

        return event.runAsync(() -> {
            try {
                handleAsync(event).ifPresent(event::setResultProperty);
            } catch (DataRequestException e) {
                logger.debug(e);
            }
        });
    }

    private boolean handleSync(SRLoginProfileEvent<R> event) {
        return settings.getProperty(AdvancedConfig.DISABLE_ON_JOIN_SKINS) || (settings.getProperty(LoginConfig.NO_SKIN_IF_LOGIN_CANCELED) && event.isCancelled());
    }

    private Optional<SkinProperty> handleAsync(SRLoginProfileEvent<R> event) throws DataRequestException {
        try {
            atomicAdapter.get().migrateLegacyPlayer(event.getPlayerName(), event.getPlayerUniqueId());
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
        }

        return playerStorage.getSkinForPlayer(event.getPlayerUniqueId(), event.getPlayerName(), event.hasOnlineProperties());
    }
}
