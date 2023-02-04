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
package net.skinsrestorer.shared.listeners;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.NotPremiumException;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.util.Pair;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.config.LoginConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class LoginProfileListenerAdapter<R> {
    private final SettingsManager settings;
    private final SkinStorageImpl skinStorage;
    private final SRLogger logger;

    public R handleLogin(SRLoginProfileEvent<R> event) {
        if (handleSync(event))
            return null;

        return event.runAsync(() -> {
            try {
                handleAsync(event).ifPresent(event::setResultProperty);
            } catch (SkinRequestException | NotPremiumException e) {
                logger.debug(e);
            }
        });
    }

    private boolean handleSync(SRLoginProfileEvent<R> event) {
        return settings.getProperty(AdvancedConfig.DISABLE_ON_JOIN_SKINS) || (settings.getProperty(LoginConfig.NO_SKIN_IF_LOGIN_CANCELED) && event.isCancelled());
    }

    private Optional<SkinProperty> handleAsync(SRLoginProfileEvent<R> event) throws SkinRequestException, NotPremiumException {
        String playerName = event.getPlayerName();
        Pair<SkinProperty, Boolean> result = skinStorage.getDefaultSkinForPlayer(playerName);

        // Skip skin if: online mode, no custom skin set, always apply not enabled and default skins for premium not enabled
        if (event.isOnline()
                && !result.getRight()
                && !settings.getProperty(LoginConfig.ALWAYS_APPLY_PREMIUM)
                && !settings.getProperty(StorageConfig.DEFAULT_SKINS_PREMIUM))
            return Optional.empty();

        return Optional.of(result.getLeft());
    }
}
