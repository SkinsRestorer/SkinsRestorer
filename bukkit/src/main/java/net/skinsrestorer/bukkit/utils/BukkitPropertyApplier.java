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
package net.skinsrestorer.bukkit.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.AuthLibHelper;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitPropertyApplier implements SkinApplyBukkitAdapter {
    private final SRLogger logger;

    @Override
    public void applyProperty(Player player, SkinProperty property) {
        try {
            PropertyMap properties = getGameProfile(player, GameProfile.class).getProperties();
            properties.removeAll(SkinProperty.TEXTURES_NAME);
            properties.put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
        } catch (ReflectiveOperationException e) {
            logger.severe("Failed to apply skin property to player %s".formatted(player.getName()), e);
        }
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(Player player) {
        try {
            return getGameProfile(player, GameProfile.class).getProperties().values()
                    .stream()
                    .map(property -> SkinProperty.tryParse(
                            AuthLibHelper.getPropertyName(property),
                            AuthLibHelper.getPropertyValue(property),
                            AuthLibHelper.getPropertySignature(property))
                    )
                    .flatMap(Optional::stream)
                    .findFirst();
        } catch (ReflectiveOperationException e) {
            logger.severe("Failed to get skin property from player %s".formatted(player.getName()), e);
            return Optional.empty();
        }
    }
}
