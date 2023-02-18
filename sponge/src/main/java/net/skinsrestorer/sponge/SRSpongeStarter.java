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
package net.skinsrestorer.sponge;

import ch.jalu.injector.Injector;
import co.aikar.commands.CommandManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.SRCommand;
import net.skinsrestorer.shared.commands.SkinCommand;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.SRPlatformStarter;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.sponge.listeners.LoginListener;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRSpongeStarter implements SRPlatformStarter {
    private final Injector injector;
    private final SRSpongeAdapter adapter;
    private final SRPlugin plugin;
    private final Game game;

    @Override
    public void pluginStartup() {
        plugin.initMineSkinAPI();

        // Init storage
        try {
            plugin.initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        SkinApplierSponge skinApplierSponge = injector.getSingleton(SkinApplierSponge.class);
        plugin.registerSkinApplier(skinApplierSponge, Player.class, Player::getName);

        // Init API
        plugin.registerAPI();

        game.getEventManager().registerListener(adapter.getPluginInstance(), ClientConnectionEvent.Auth.class, injector.newInstance(LoginListener.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = plugin.sharedInitCommands();

        manager.registerCommand(injector.getSingleton(SkinCommand.class));
        manager.registerCommand(injector.newInstance(SRCommand.class));
    }
}
