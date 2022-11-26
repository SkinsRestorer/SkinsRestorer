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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.bungee.SkinsRestorerBungee;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;

@RequiredArgsConstructor
@Getter
public class LoginListener extends SharedLoginProfileListener implements Listener {
    private final SkinsRestorerBungee plugin;

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final LoginEvent event) {
        SRLoginProfileEvent profileEvent = wrap(event);
        if (handleSync(profileEvent))
            return;

        event.registerIntent(plugin.getPluginInstance());

        plugin.runAsync(() -> {
            try {
                handleAsync(profileEvent).ifPresent(property ->
                        plugin.getSkinApplierBungee().applySkin(property, (InitialHandler) event.getConnection()));
            } catch (SkinRequestException e) {
                plugin.getLogger().debug(e);
            }

            event.completeIntent(plugin.getPluginInstance());
        });
    }

    private SRLoginProfileEvent wrap(LoginEvent event) {
        return new SRLoginProfileEvent() {
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
        };
    }
}
