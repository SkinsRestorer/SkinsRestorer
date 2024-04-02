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
package net.skinsrestorer.bukkit.paper;

import com.destroystokyo.paper.profile.ProfileProperty;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import javax.inject.Inject;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PaperPlayerJoinEvent implements Listener {
    private final LoginProfileListenerAdapter<Void> adapter;

    public static boolean isAvailable() {
        try {
            // Paper API method
            AsyncPlayerPreLoginEvent.class.getMethod("getPlayerProfile");
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        adapter.handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(AsyncPlayerPreLoginEvent event) {
        return new SRLoginProfileEvent<>() {
            @Override
            public boolean hasOnlineProperties() {
                return !event.getPlayerProfile().getProperties().isEmpty();
            }

            @Override
            public UUID getPlayerUniqueId() {
                return event.getUniqueId();
            }

            @Override
            public String getPlayerName() {
                return event.getName();
            }

            @Override
            public boolean isCancelled() {
                return event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED;
            }

            @Override
            public void setResultProperty(SkinProperty property) {
                event.getPlayerProfile().setProperty(new ProfileProperty(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
            }

            @Override
            public Void runAsync(Runnable runnable) {
                runnable.run();
                return null;
            }
        };
    }
}
