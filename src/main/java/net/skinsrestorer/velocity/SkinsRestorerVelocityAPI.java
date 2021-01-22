/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.velocity;

import com.google.common.annotations.Beta;
import com.velocitypowered.api.proxy.Player;
import net.skinsrestorer.shared.interfaces.ISkinsRestorerAPI;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.MojangAPI;
import net.skinsrestorer.shared.utils.PlayerWrapper;
import net.skinsrestorer.shared.utils.SkinsRestorerAPI;

/**
 * Created by McLive on 10.11.2019.
 */
public class SkinsRestorerVelocityAPI extends SkinsRestorerAPI implements ISkinsRestorerAPI<Player> {
    private final SkinsRestorer plugin;

    public SkinsRestorerVelocityAPI(SkinsRestorer plugin, MojangAPI mojangAPI, SkinStorage skinStorage) {
        super(mojangAPI, skinStorage);
        this.plugin = plugin;
    }

    // Todo: We need to refactor applySkin through all platforms to behave the same!
    @Beta
    @Override
    public void applySkin(PlayerWrapper player, Object props) {
        this.applySkin(player);
    }

    @Beta
    @Override
    public void applySkin(PlayerWrapper player) {
        plugin.getSkinApplierVelocity().applySkin(player, this.getSkinName(player.get(Player.class).getUsername()));
    }
}
