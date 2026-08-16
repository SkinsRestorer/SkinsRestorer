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
package net.skinsrestorer.bukkit;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.paper.PaperSkinApplier;
import net.skinsrestorer.bukkit.refresher.SkinRefresher;
import net.skinsrestorer.bukkit.spigot.SpigotPassengerUtil;
import net.skinsrestorer.bukkit.utils.MultiPaperUtil;
import net.skinsrestorer.bukkit.utils.SkinApplyBukkitAdapter;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleWire;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierBukkit implements SkinApplierAccess<Player> {
    private final SkinApplyBukkitAdapter applyAdapter;
    private final SRBukkitAdapter adapter;
    private final Server server;
    private final EventBusImpl eventBus;
    private final SkinRefresher refresh;
    private final SpigotPassengerUtil passengerUtil;
    private final SettingsManager settingsManager;

    @Override
    public void applySkin(Player player, SkinProperty property) {
        if (!player.isOnline()) {
            return;
        }

        adapter.runAsync(() -> {
            // run the skin apply event
            SkinApplyEventImpl applyEvent = new SkinApplyEventImpl(player, property);
            eventBus.callEvent(applyEvent);
            if (applyEvent.isCancelled()) {
                return;
            }

            // delay 1 server tick so we override online-mode
            adapter.runSyncToPlayer(player, () -> applySkinSync(player, applyEvent.getProperty()));
        });
    }

    public void applySkinSync(Player player, SkinProperty property) {
        if (!player.isOnline()) {
            return;
        }

        // We do things with passengers to avoid desync issues with riding entities
        if (SpigotPassengerUtil.isAvailable()) {
            passengerUtil.ejectPassengers(player);
        }

        if (refresh.needsManualPropertyApply()) {
            // Otherwise we use the SkinsRestorer adapter to apply the skin
            applyAdapter.applyProperty(player, property);
        }

        if (refresh.needsManualOtherRefresh()) {
            if (settingsManager.getProperty(AdvancedConfig.TELEPORT_REFRESH)) {
                teleportOtherRefresh(player);
            } else {
                normalOtherRefresh(player);
            }
        }

        // Refresh the players own skin
        refresh.refresh(player, property);
        refreshSkinShufflePlayerEntries(player);
    }

    private void normalOtherRefresh(Player player) {
        for (Player otherPlayer : getSeenByPlayers(player)) {
            // Force player to be re-added to the player-list of every player on the server
            hideAndShow(otherPlayer, player);
        }
    }

    private List<? extends Player> getSeenByPlayers(Player player) {
        // Do not hide the player from itself or do anything if the other player cannot see the player
        return getOnlinePlayers()
                .stream()
                .filter(other -> !other.getUniqueId().equals(player.getUniqueId()))
                .filter(other -> other.canSee(player))
                .toList();
    }

    @SuppressWarnings("deprecation")
    private void hideAndShow(Player player, Player other) {
        try {
            player.hidePlayer(adapter.getPluginInstance(), other);
        } catch (NoSuchMethodError ignored) {
            // Backwards compatibility
            player.hidePlayer(other);
        }

        try {
            player.showPlayer(adapter.getPluginInstance(), other);
        } catch (NoSuchMethodError ignored) {
            // Backwards compatibility
            player.showPlayer(other);
        }
    }

    private void teleportOtherRefresh(Player player) {
        for (Player otherPlayer : getSeenByPlayers(player)) {
            // Force player to be re-added to the player-list of every player on the server
            // This however sends only the player list packet, while the hide/show method
            // de-spawns the player and sends the spawn packet
            refresh.resendInfoPackets(player, otherPlayer);
        }

        Location location = player.getLocation();
        // Teleport the player to a far away location
        player.teleport(new Location(server.getWorlds()
                .stream()
                .filter(world -> world != location.getWorld())
                .findFirst()
                .orElse(location.getWorld()),
                0, 0, 0));
        // Teleport the player back to the original location
        player.teleport(location);
    }

    private Collection<? extends Player> getOnlinePlayers() {
        try {
            return MultiPaperUtil.getOnlinePlayers();
        } catch (Throwable e) { // Catch all errors and fallback to bukkit
            return server.getOnlinePlayers();
        }
    }

    private void refreshSkinShufflePlayerEntries(Player player) {
        if (!settingsManager.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)) {
            return;
        }

        byte[] payload = SkinShuffleWire.createRefreshPlayerListEntryPayload(player.getEntityId());
        for (Player otherPlayer : getSeenByPlayers(player)) {
            sendSkinShufflePacket(otherPlayer, SkinShuffleChannels.REFRESH_PLAYER_LIST_ENTRY, payload);
        }
        sendSkinShufflePacket(player, SkinShuffleChannels.REFRESH_PLAYER_LIST_ENTRY, payload);
    }

    private void sendSkinShufflePacket(Player player, String channel, byte[] payload) {
        player.sendPluginMessage(adapter.getPluginInstance(), channel, payload);
    }
}
