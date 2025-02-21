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
package net.skinsrestorer.shared.subjects.permissions;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinPermissionManager {
    private final SettingsManager settings;

    public Optional<Message> canSetSkin(SRCommandSender sender, String skinInput) {
        if (isPerSkinPermissions()
                && !sender.hasPermission(PermissionRegistry.forSkin(skinInput.toLowerCase(Locale.ROOT)))
                && (!sender.hasPermission(PermissionRegistry.OWN_SKIN)
                || !(sender instanceof SRPlayer player)
                || !skinInput.equalsIgnoreCase(player.getName()))) {
            return Optional.of(Message.PLAYER_HAS_NO_PERMISSION_SKIN);
        }

        if (isDisabledSkin(skinInput) && !sender.hasPermission(PermissionRegistry.BYPASS_DISABLED)) {
            return Optional.of(Message.ERROR_SKIN_DISABLED);
        }

        if (ValidationUtil.validSkinUrl(skinInput)) {
            if (!sender.hasPermission(PermissionRegistry.SKIN_SET_URL)) {
                return Optional.of(Message.PLAYER_HAS_NO_PERMISSION_URL);
            }

            if (!allowedSkinUrl(skinInput)) {
                return Optional.of(Message.ERROR_SKINURL_DISALLOWED);
            }
        }

        return Optional.empty();
    }

    private boolean isPerSkinPermissions() {
        if (settings.getProperty(CommandConfig.PER_SKIN_PERMISSIONS)) {
            return settings.getProperty(CommandConfig.PER_SKIN_PERMISSIONS_CONSENT).equalsIgnoreCase(CommandConfig.CONSENT_MESSAGE);
        } else {
            return false;
        }
    }

    private boolean isDisabledSkin(String skinName) {
        return settings.getProperty(CommandConfig.DISABLED_SKINS_ENABLED)
                && settings.getProperty(CommandConfig.DISABLED_SKINS).stream().anyMatch(skinName::equalsIgnoreCase);
    }

    private boolean allowedSkinUrl(String url) {
        return !settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_ENABLED)
                || settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_LIST)
                .stream()
                .anyMatch(url::startsWith);
    }
}
