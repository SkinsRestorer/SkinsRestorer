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
package net.skinsrestorer.velocity.listener;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.velocity.SkinApplierVelocity;

public class GameProfileRequest extends SharedLoginProfileListener<EventTask> {
    private final SkinApplierVelocity skinApplier;

    public GameProfileRequest(SkinStorage skinStorage, SettingsManager settings, SkinApplierVelocity skinApplier, SRLogger logger) {
        super(settings, skinStorage, logger);
        this.skinApplier = skinApplier;
    }

    @Subscribe
    public EventTask onGameProfileRequest(GameProfileRequestEvent event) {
        return handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<EventTask> wrap(GameProfileRequestEvent event) {
        return new SRLoginProfileEvent<EventTask>() {
            @Override
            public boolean isOnline() {
                return event.isOnlineMode();
            }

            @Override
            public String getPlayerName() {
                return event.getUsername();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void setResultProperty(IProperty property) {
                event.setGameProfile(skinApplier.updateProfileSkin(event.getGameProfile(), property));
            }

            @Override
            public EventTask runAsync(Runnable runnable) {
                return EventTask.async(runnable);
            }
        };
    }
}
