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
package net.skinsrestorer.bungee.listeners;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.SRBungeeAdapter;
import net.skinsrestorer.bungee.SkinApplierBungee;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;

import javax.inject.Inject;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoginListener implements Listener {
    private final SRBungeeAdapter plugin;
    private final SkinApplierBungee skinApplier;
    private final LoginProfileListenerAdapter<Void> adapter;

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final LoginEvent event) {
        adapter.handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(LoginEvent event) {
        return new SRLoginProfileEvent<>() {
            @Override
            public boolean hasOnlineProperties() {
                return event.getConnection().isOnlineMode();
            }

            @Override
            public UUID getPlayerUniqueId() {
                return event.getConnection().getUniqueId();
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
            public void setResultProperty(SkinProperty property) {
                skinApplier.applySkin(property, event.getConnection());
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
