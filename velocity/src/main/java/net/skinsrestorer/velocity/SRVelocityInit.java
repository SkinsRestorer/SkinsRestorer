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
package net.skinsrestorer.velocity;

import ch.jalu.injector.Injector;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.miniplaceholders.SRMiniPlaceholdersAPIExpansion;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRProxyPlatformInit;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.velocity.listener.AdminInfoListener;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.listener.ProxyMessageListener;
import net.skinsrestorer.velocity.wrapper.WrapperVelocity;

import javax.inject.Inject;

@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRVelocityInit implements SRProxyPlatformInit {
    private final Injector injector;
    private final SRLogger logger;
    private final SRVelocityAdapter adapter;
    private final SRPlugin plugin;
    private final ProxyServer proxy;
    private final WrapperVelocity wrapper;

    @Override
    public void initSkinApplier() {
        plugin.registerSkinApplier(injector.getSingleton(SkinApplierVelocity.class), Player.class, wrapper);
    }

    @Override
    public void initLoginProfileListener() {
        proxy.getEventManager().register(adapter.pluginInstance(), injector.newInstance(GameProfileRequest.class));
    }

    @Override
    public void initAdminInfoListener() {
        proxy.getEventManager().register(adapter.pluginInstance(), injector.newInstance(AdminInfoListener.class));
    }

    @Override
    public void initMessageChannel() {
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from(SRHelpers.MESSAGE_CHANNEL));
        proxy.getEventManager().register(adapter.pluginInstance(), PluginMessageEvent.class, injector.getSingleton(ProxyMessageListener.class));
    }

    @Override
    public void placeholderSetupHook() {
        if (adapter.getPluginInfo("miniplaceholders").isPresent()) {
            try {
                new SRMiniPlaceholdersAPIExpansion<>(
                        adapter,
                        Player.class,
                        wrapper::player
                ).register();
                logger.info("MiniPlaceholders expansion registered!");
            } catch (Throwable t) {
                logger.severe("Failed to load MiniPlaceholders expansion! Please check if both SkinsRestorer and MiniPlaceholders are up-to-date.", t);
            }
        }
    }
}
