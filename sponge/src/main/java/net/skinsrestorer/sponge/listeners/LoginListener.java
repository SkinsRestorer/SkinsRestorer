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
package net.skinsrestorer.sponge.listeners;

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.sponge.SkinApplierSponge;
import net.skinsrestorer.sponge.SkinsRestorerSponge;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Auth;

@Getter
public class LoginListener extends SharedLoginProfileListener<Void> implements EventListener<ClientConnectionEvent.Auth> {
    private final SkinsRestorerSponge plugin;
    private final SkinApplierSponge skinApplier;

    public LoginListener(SkinStorage skinStorage, SettingsManager settings, SkinsRestorerSponge plugin, SRLogger logger, SkinApplierSponge skinApplier) {
        super(settings, skinStorage, logger);
        this.plugin = plugin;
        this.skinApplier = skinApplier;
    }

    @Override
    public void handle(@NotNull Auth event) {
        handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(Auth event) {
        return new SRLoginProfileEvent<Void>() {
            @Override
            public boolean isOnline() {
                return Sponge.getServer().getOnlineMode();
            }

            @Override
            public String getPlayerName() {
                return event.getProfile().getName().orElseThrow(() -> new RuntimeException("Could not get player name!"));
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void setResultProperty(IProperty property) {
                skinApplier.updateProfileSkin(event.getProfile(), property);
            }

            @Override
            public Void runAsync(Runnable runnable) {
                return null;
            }
        };
    }
}
