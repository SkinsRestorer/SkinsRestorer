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
package net.skinsrestorer.shared.listeners;

import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.util.Pair;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;

import java.util.Optional;

public abstract class SharedLoginProfileListener {
    protected boolean handleSync(SRLoginProfileEvent event) {
        return Config.DISABLE_ON_JOIN_SKINS || (Config.NO_SKIN_IF_LOGIN_CANCELED && event.isCancelled());
    }

    protected Optional<IProperty> handleAsync(SRLoginProfileEvent event) throws SkinRequestException {
        ISRPlugin plugin = getPlugin();
        String playerName = event.getPlayerName();
        Pair<IProperty, Boolean> result = plugin.getSkinStorage().getDefaultSkinForPlayer(playerName);

        // Skip skin if: online mode, no custom skin set, always apply not enabled and default skins for premium not enabled
        if (event.isOnline()
                && !result.getRight()
                && !Config.ALWAYS_APPLY_PREMIUM
                && !Config.DEFAULT_SKINS_PREMIUM)
            return Optional.empty();

        return Optional.of(result.getLeft());
    }

    protected abstract ISRPlugin getPlugin();
}
