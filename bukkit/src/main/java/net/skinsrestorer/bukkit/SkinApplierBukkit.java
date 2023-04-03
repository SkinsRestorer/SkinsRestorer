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
package net.skinsrestorer.bukkit;

import ch.jalu.configme.SettingsManager;
import io.papermc.lib.PaperLib;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.multipaper.MultiPaperUtil;
import net.skinsrestorer.bukkit.paper.PaperSkinApplier;
import net.skinsrestorer.bukkit.skinrefresher.MappingSpigotSkinRefresher;
import net.skinsrestorer.bukkit.skinrefresher.PaperSkinRefresher;
import net.skinsrestorer.bukkit.skinrefresher.SpigotSkinRefresher;
import net.skinsrestorer.bukkit.spigot.SpigotPassengerUtil;
import net.skinsrestorer.bukkit.spigot.SpigotUtil;
import net.skinsrestorer.bukkit.utils.*;
import net.skinsrestorer.bukkit.v1_7.BukkitLegacyPropertyApplier;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.serverinfo.SemanticVersion;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Consumer;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierBukkit implements SkinApplierAccess<Player> {
    private static final boolean IS_MODERN_AUTH_LIB = ReflectionUtil.classExists("com.mojang.authlib.GameProfile");
    @Getter
    private static final SkinApplyBukkitAdapter applyAdapter = selectSkinApplyAdapter();
    private final SRBukkitAdapter adapter;
    private final SRLogger logger;
    private final SettingsManager settings;
    private final Server server;
    private final EventBusImpl eventBus;
    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private Consumer<Player> refresh;

    private static SkinApplyBukkitAdapter selectSkinApplyAdapter() {
        if (IS_MODERN_AUTH_LIB) {
            return new BukkitPropertyApplier();
        } else {
            return new BukkitLegacyPropertyApplier();
        }
    }

    protected Consumer<Player> detectRefresh(Server server) throws InitializeException {
        if (isPaper()) {
            // force SpigotSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
            // Ran with #getPlugin() != null instead of #isPluginEnabled() as older Spigot builds return false during the login process even if enabled
            boolean viaVersionExists = adapter.isPluginEnabled("ViaVersion");
            boolean protocolSupportExists = adapter.isPluginEnabled("ProtocolSupport");
            if (viaVersionExists || protocolSupportExists) {
                logger.debug(SRLogLevel.WARNING, "Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing SpigotSkinRefresher");
                return selectSpigotRefresher(server);
            }

            // use PaperSkinRefresher if no VersionHack plugin found
            try {
                return new PaperSkinRefresher(logger);
            } catch (NoMappingException e) {
                throw e;
            } catch (InitializeException e) {
                logger.severe("PaperSkinRefresher failed! (Are you using hybrid software?) Only limited support can be provided. Falling back to SpigotSkinRefresher.");
            }
        }

        return selectSpigotRefresher(server);
    }

    private Consumer<Player> selectSpigotRefresher(Server server) throws InitializeException {
        if (NMSVersion.SERVER_VERSION.isNewerThan(new SemanticVersion(1, 17, 1))) {
            return new MappingSpigotSkinRefresher(adapter, logger, server);
        } else return new SpigotSkinRefresher(adapter, logger);
    }

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
            adapter.runSync(() -> {
                applyAdapter.applyProperty(player, applyEvent.getProperty());

                adapter.runAsync(() -> updateSkin(player, applyEvent.getProperty()));
            });
        });
    }

    /**
     * Instantly updates player's skin
     *
     * @param player - Player
     */
    @SuppressWarnings("deprecation")
    public void updateSkin(Player player, SkinProperty property) {
        if (!player.isOnline()) {
            return;
        }

        adapter.runSync(() -> {
            ejectPassengers(player);

            if (PaperSkinApplier.hasProfileMethod()) {
                System.out.println("hasProfileMethod");
                PaperSkinApplier.applySkin(player, property);
                return;
            }

            for (Player ps : getOnlinePlayers()) {
                // Some older spigot versions only support hidePlayer(player)
                try {
                    ps.hidePlayer(adapter.getPluginInstance(), player);
                } catch (NoSuchMethodError ignored) {
                    ps.hidePlayer(player);
                }

                try {
                    ps.showPlayer(adapter.getPluginInstance(), player);
                } catch (NoSuchMethodError ignored) {
                    ps.showPlayer(player);
                }
            }

            refresh.accept(player);
        });
    }

    private void ejectPassengers(Player player) {
        if (PaperLib.isSpigot() && SpigotUtil.hasPassengerMethods()) {
            Entity vehicle = player.getVehicle();

            SpigotPassengerUtil.refreshPassengers(adapter.getPluginInstance(), player, vehicle, settings);
        }
    }

    private boolean isPaper() {
        if (PaperLib.isPaper() && NMSVersion.SERVER_VERSION.isNewerThan(new SemanticVersion(1, 11, 2))) {
            if (hasPaperMethods()) {
                return true;
            } else {
                logger.debug(SRLogLevel.WARNING, "Paper detected, but the methods are missing. Disabling Paper Refresher.");
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean hasPaperMethods() {
        try {
            BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
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
