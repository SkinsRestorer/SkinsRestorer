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
package net.skinsrestorer.bukkit;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.utils.MultiPaperUtil;
import net.skinsrestorer.bukkit.paper.PaperSkinApplier;
import net.skinsrestorer.bukkit.refresher.SkinRefresher;
import net.skinsrestorer.bukkit.spigot.SpigotPassengerUtil;
import net.skinsrestorer.bukkit.spigot.SpigotUtil;
import net.skinsrestorer.bukkit.utils.SkinApplyBukkitAdapter;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.info.ClassInfo;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collection;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierBukkit implements SkinApplierAccess<Player> {
    private final SkinApplyBukkitAdapter applyAdapter;
    private final SRBukkitAdapter adapter;
    private final SettingsManager settings;
    private final Server server;
    private final EventBusImpl eventBus;
    private final SkinRefresher refresh;

    @Override
    public void applySkin(Player player, SkinProperty property) {
        if (!player.isOnline()) {
            return;
        }

        adapter.runAsync(() -> {
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

        ejectPassengers(player);

        if (ReflectionUtil.classExists("com.destroystokyo.paper.profile.PlayerProfile")
                && PaperSkinApplier.hasProfileMethod()) {
            PaperSkinApplier.applySkin(player, property);
            return;
        }

        applyAdapter.applyProperty(player, property);

        for (Player otherPlayer : getOnlinePlayers()) {
            // Do not hide the player from itself or do anything if the other player cannot see the player
            if (otherPlayer.getUniqueId().equals(player.getUniqueId())
                    || !otherPlayer.canSee(player)) {
                continue;
            }

            // Force player to be re-added to the player-list of every player on the server
            hideAndShow(otherPlayer, player);
        }

        // Refresh the players own skin
        refresh.refresh(player);
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

    private void ejectPassengers(Player player) {
        if (ClassInfo.get().isSpigot() && SpigotUtil.hasPassengerMethods()) {
            SpigotPassengerUtil.ejectPassengers(adapter.getSchedulerProvider(), player, settings);
        }
    }

    private Collection<? extends Player> getOnlinePlayers() {
        try {
            return MultiPaperUtil.getOnlinePlayers();
        } catch (Throwable e) { // Catch all errors and fallback to bukkit
            return server.getOnlinePlayers();
        }
    }
}
