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
package net.skinsrestorer.skinshuffle;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleSupport;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkinShuffleSupportTest {
    @Mock
    private SettingsManager settings;
    @Mock
    private Injector injector;
    @Mock
    private SkinStorage skinStorage;
    @Mock
    private PlayerStorage playerStorage;
    @Mock
    private SRPlatformAdapter adapter;
    @Mock
    private SRLogger logger;
    @Mock
    private SharedSkinApplier<Object> skinApplier;
    @Mock
    private SRPlayer player;

    @Test
    void shouldPersistAndApplySkinRefresh() {
        SkinShuffleSupport support = new SkinShuffleSupport(settings, injector, skinStorage, playerStorage, adapter, logger);
        UUID uniqueId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Object platformPlayer = new Object();
        SkinProperty property = SkinProperty.of("value", "signature");
        String expectedSkinId = "skinshuffle-11111111-2222-3333-4444-555555555555";

        when(settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)).thenReturn(true);
        when(player.hasPermission(PermissionRegistry.SKIN_SET)).thenReturn(true);
        when(player.getUniqueId()).thenReturn(uniqueId);
        when(player.getAs(Object.class)).thenReturn(platformPlayer);
        when(injector.getSingleton(SharedSkinApplier.class)).thenReturn(skinApplier);

        support.handleSkinRefresh(player, property);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(adapter).runAsync(taskCaptor.capture());
        verifyNoInteractions(skinStorage, playerStorage, skinApplier);

        taskCaptor.getValue().run();

        verify(skinStorage).setCustomSkinData(expectedSkinId, property);
        verify(playerStorage).setSkinIdOfPlayer(uniqueId, SkinIdentifier.ofCustom(expectedSkinId));
        verify(skinApplier).applySkin(platformPlayer, property);
    }

    @Test
    void shouldRejectSkinRefreshWithoutPermission() {
        SkinShuffleSupport support = new SkinShuffleSupport(settings, injector, skinStorage, playerStorage, adapter, logger);
        SkinProperty property = SkinProperty.of("value", "signature");

        when(settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)).thenReturn(true);
        when(player.hasPermission(PermissionRegistry.SKIN_SET)).thenReturn(false);

        support.handleSkinRefresh(player, property);

        verifyNoInteractions(skinStorage, playerStorage, injector, adapter);
    }

    @Test
    void shouldIgnoreInvalidWirePayload() {
        SkinShuffleSupport support = new SkinShuffleSupport(settings, injector, skinStorage, playerStorage, adapter, logger);

        when(settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)).thenReturn(true);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getUniqueId()).thenReturn(UUID.fromString("11111111-2222-3333-4444-555555555555"));

        support.handleSkinRefresh(player, new byte[]{1, 0});

        verify(logger).warning(contains("Ignoring invalid SkinShuffle skin refresh payload"));
        verifyNoInteractions(skinStorage, playerStorage, injector, adapter);
    }
}
