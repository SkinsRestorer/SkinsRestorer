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
package net.skinsrestorer.shared.listeners;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.listeners.event.SRServerConnectedEvent;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.update.UpdateCheckInit;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class AdminInfoListenerAdapter {
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final UpdateCheckInit updateCheckInit;

    public void handleConnect(SRServerConnectedEvent event) {
        SRPlayer player = event.getPlayer();

        adapter.runAsync(() -> {
            if (plugin.isOutdated() && updateCheckInit.getDownloader().isEmpty() && player.hasPermission(PermissionRegistry.SR)) {
                player.sendMessage(Message.OUTDATED, Placeholder.parsed("platform", adapter.getPlatform().getPlatformDescription()));
            }

            int version = SRHelpers.getJavaVersion();
            if (version < 17 && player.hasPermission(PermissionRegistry.SR)) {
                player.sendMessage(Message.UNSUPPORTED_JAVA, Placeholder.parsed("version", String.valueOf(version)), Placeholder.parsed("platform", adapter.getPlatform().getPlatformDescription()));
            }
        });
    }
}
