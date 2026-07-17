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
package net.skinsrestorer.fand;

import ch.jalu.injector.Injector;
import io.fand.api.entity.Player;
import io.fand.api.event.player.AsyncPlayerPreLoginEvent;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.plugin.PluginContext;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.fand.placeholder.FandPlaceholderExpansion;
import net.skinsrestorer.fand.wrapper.WrapperFand;
import net.skinsrestorer.shared.listeners.AdminInfoListenerAdapter;
import net.skinsrestorer.shared.listeners.SRServerMessageAdapter;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.shared.log.SRChatColor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlatformInit;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.subjects.permissions.PermissionGroup;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRFandInit implements SRServerPlatformInit {
    private final SRPlugin plugin;
    private final SRFandAdapter adapter;
    private final Injector injector;
    private final PluginContext context;
    private final SRLogger logger;
    private final WrapperFand wrapper;

    @Override
    public void initSkinApplier() {
        plugin.registerSkinApplier(injector.getSingleton(SkinApplierFand.class), Player.class, wrapper);
        logger.info(SRChatColor.GREEN + "Running on Fand " + SRChatColor.YELLOW
                + adapter.getPlatformVersion() + SRChatColor.GREEN + ".");
    }

    @Override
    public void initLoginProfileListener() {
        FandLoginProfileListener listener = injector.getSingleton(FandLoginProfileListener.class);
        context.events().subscribe(AsyncPlayerPreLoginEvent.class, listener::login);
    }

    @Override
    public void initAdminInfoListener() {
        AdminInfoListenerAdapter adminInfo = injector.getSingleton(AdminInfoListenerAdapter.class);
        context.events().subscribe(PlayerJoinEvent.class,
                event -> adminInfo.handleConnect(() -> wrapper.player(event.player())));
    }

    @Override
    public void initPermissions() {
        for (PermissionRegistry permission : PermissionRegistry.values()) {
            context.permissions().register(new PermissionDescriptor(
                    permission.getPermission().getPermissionString(),
                    PermissionDefault.OPERATOR));
        }

        for (PermissionGroup group : PermissionGroup.values()) {
            Map<String, Boolean> children = new HashMap<>();
            mergePermissions(group, children);
            PermissionDefault defaultAccess = group == PermissionGroup.PLAYER
                    ? PermissionDefault.TRUE
                    : PermissionDefault.OPERATOR;
            context.permissions().register(new PermissionDescriptor(
                    group.getBasePermission().getPermissionString(), defaultAccess, children));
            context.permissions().register(new PermissionDescriptor(
                    group.getWildcard().getPermissionString(), defaultAccess, children));
        }
    }

    private void mergePermissions(PermissionGroup group, Map<String, Boolean> children) {
        for (PermissionRegistry permission : group.getPermissions()) {
            children.put(permission.getPermission().getPermissionString(), true);
        }
        for (PermissionGroup parent : group.getParents()) {
            mergePermissions(parent, children);
        }
    }

    @Override
    public void initGUIListener() {
        // Fand's GuiService owns the inventory event lifecycle.
    }

    @Override
    public void initMessageChannel() {
        SRServerMessageAdapter messageAdapter = injector.getSingleton(SRServerMessageAdapter.class);
        context.pluginMessaging().register(
                SRFandAdapter.MESSAGE_CHANNEL,
                PluginMessageDirection.BIDIRECTIONAL,
                (player, channel, payload) -> messageAdapter.handlePluginMessage(new SRServerMessageEvent() {
                    @Override
                    public SRServerPlayer getPlayer() {
                        return wrapper.player(player);
                    }

                    @Override
                    public byte[] getData() {
                        return payload;
                    }

                    @Override
                    public String getChannel() {
                        return channel.key().asString();
                    }
                }));
    }

    @Override
    public void placeholderSetupHook() {
        injector.getSingleton(FandPlaceholderExpansion.class).register();
    }
}
