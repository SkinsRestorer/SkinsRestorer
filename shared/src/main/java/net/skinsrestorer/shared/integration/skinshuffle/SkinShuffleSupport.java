/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.shared.integration.skinshuffle;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinShuffleSupport {
    private static final String CUSTOM_SKIN_PREFIX = "skinshuffle-";
    private final SettingsManager settings;
    private final Injector injector;
    private final SkinStorage skinStorage;
    private final PlayerStorage playerStorage;
    private final SRPlatformAdapter adapter;
    private final SRLogger logger;

    public boolean isEnabled() {
        return settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT);
    }

    public void handleSkinRefresh(SRPlayer player, byte[] data) {
        if (!isEnabled()) {
            return;
        }

        Optional<SkinProperty> property = SkinShuffleWire.decodeSkinRefreshPayload(data);
        if (property.isEmpty()) {
            logger.warning("Ignoring invalid SkinShuffle skin refresh payload from %s (%s)."
                    .formatted(player.getName(), player.getUniqueId()));
            return;
        }

        handleSkinRefresh(player, property.get());
    }

    public void handleSkinRefresh(SRPlayer player, SkinProperty property) {
        if (!isEnabled()) {
            return;
        }

        if (!player.hasPermission(PermissionRegistry.SKIN_SET)) {
            player.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_SKIN);
            return;
        }

        UUID playerUniqueId = player.getUniqueId();
        Object platformPlayer = player.getAs(Object.class);
        adapter.runAsync(() -> {
            String skinId = getSkinIdentifier(playerUniqueId);
            skinStorage.setCustomSkinData(skinId, property);
            playerStorage.setSkinIdOfPlayer(playerUniqueId, SkinIdentifier.ofCustom(skinId));
            getSkinApplier().applySkin(platformPlayer, property);
        });
    }

    public String getSkinIdentifier(UUID playerUniqueId) {
        return CUSTOM_SKIN_PREFIX + playerUniqueId.toString().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private SharedSkinApplier<Object> getSkinApplier() {
        return injector.getSingleton(SharedSkinApplier.class);
    }
}
