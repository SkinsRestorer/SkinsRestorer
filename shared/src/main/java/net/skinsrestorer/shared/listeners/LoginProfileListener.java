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

import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;

import java.util.Optional;

public abstract class LoginProfileListener {
	protected boolean handleSync(LoginProfileEvent event) {
		return Config.DISABLE_ON_JOIN_SKINS || (Config.NO_SKIN_IF_LOGIN_CANCELED && event.isCancelled());
	}

	// TODO: add default skinurl support
	protected Optional<String> handleAsync(LoginProfileEvent event) {
		ISRPlugin plugin = getPlugin();
		String playerName = event.getPlayerName();
		Optional<String> skin = plugin.getSkinStorage().getSkinOfPlayer(playerName);

		// Skip players if: OnlineMode & no skin set & enabled & DefaultSkins.premium false
		if (event.isOnline()
				&& !skin.isPresent()
				&& !Config.ALWAYS_APPLY_PREMIUM
				&& !Config.DEFAULT_SKINS_PREMIUM)
			return Optional.empty();

		// Get default skin if enabled
		if (Config.DEFAULT_SKINS_ENABLED)
			skin = Optional.ofNullable(plugin.getSkinStorage().getDefaultSkinName(playerName));

		return Optional.of(skin.orElse(playerName));
	}

	protected abstract ISRPlugin getPlugin();
}
