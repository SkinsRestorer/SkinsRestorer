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
package net.skinsrestorer.bungee.listeners;

import ch.jalu.configme.SettingsManager;
import lombok.Getter;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bungee.SkinApplierBungeeShared;
import net.skinsrestorer.bungee.SkinsRestorerBungee;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;

public class LoginListener extends SharedLoginProfileListener<Void> implements Listener {
    private final SkinsRestorerBungee plugin;
    private final SkinApplierBungeeShared skinApplier;

    @Inject
    public LoginListener(SkinStorage skinStorage, SettingsManager settings, SkinsRestorerBungee plugin, SkinApplierBungeeShared skinApplier, SRLogger logger) {
        super(settings, skinStorage, logger);
        this.plugin = plugin;
        this.skinApplier = skinApplier;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final LoginEvent event) {
        handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(LoginEvent event) {
        return new SRLoginProfileEvent<Void>() {
            @Override
            public boolean isOnline() {
                return event.getConnection().isOnlineMode();
            }

            @Override
            public String getPlayerName() {
                return event.getConnection().getName();
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void setResultProperty(IProperty property) {
                skinApplier.applySkin(property, (InitialHandler) event.getConnection());
            }

            @Override
            public Void runAsync(Runnable runnable) {
                event.registerIntent(plugin.getPluginInstance());

                plugin.runAsync(() -> {
                    runnable.run();

                    event.completeIntent(plugin.getPluginInstance());
                });
                return null;
            }
        };
    }
}
