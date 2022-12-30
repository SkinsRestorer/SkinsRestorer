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
package net.skinsrestorer.paper;

import com.destroystokyo.paper.profile.ProfileProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PaperPlayerJoinEvent implements Listener {
    @Inject
    private LoginProfileListenerAdapter<Void> adapter;

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        adapter.handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(AsyncPlayerPreLoginEvent event) {
        return new SRLoginProfileEvent<Void>() {
            @Override
            public boolean isOnline() {
                return !UUID.nameUUIDFromBytes(("OfflinePlayer:" + getPlayerName()).getBytes(StandardCharsets.UTF_8)).equals(event.getPlayerProfile().getId());
            }

            @Override
            public String getPlayerName() {
                return event.getPlayerProfile().getName();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void setResultProperty(IProperty property) {
                event.getPlayerProfile().setProperty(new ProfileProperty(property.getName(), property.getValue(), property.getSignature()));
            }

            @Override
            public Void runAsync(Runnable runnable) {
                runnable.run();
                return null;
            }
        };
    }
}
