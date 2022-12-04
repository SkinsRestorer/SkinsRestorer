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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import net.skinsrestorer.shared.commands.SharedSkinCommand;
import net.skinsrestorer.shared.injector.GetPlayerMethod;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.listeners.SRPluginMessageEvent;
import net.skinsrestorer.shared.listeners.SharedPluginMessageListener;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.velocity.command.SkinCommand;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Function;

public class PluginMessageListener extends SharedPluginMessageListener {
    @Inject
    public PluginMessageListener(SRLogger logger, SkinStorage skinStorage, SkinCommand skinCommand,
                                 @GetPlayerMethod Function<String, Optional<ISRProxyPlayer>> playerGetter) {
        super(logger, skinStorage, skinCommand, playerGetter);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        handlePluginMessage(new SRPluginMessageEvent() {
            @Override
            public boolean isCancelled() {
                return !event.getResult().isAllowed();
            }

            @Override
            public void setCancelled(boolean cancelled) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }

            @Override
            public byte[] getData() {
                return event.getData();
            }

            @Override
            public boolean isServerConnection() {
                return event.getSource() instanceof ServerConnection;
            }

            @Override
            public String getTag() {
                return event.getIdentifier().getId();
            }
        });
    }
}
