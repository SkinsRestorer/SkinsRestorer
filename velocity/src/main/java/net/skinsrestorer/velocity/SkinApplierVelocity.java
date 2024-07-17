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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.velocity.wrapper.WrapperVelocity;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierVelocity implements SkinApplierAccess<Player> {
    private final WrapperVelocity wrapper;
    private final EventBusImpl eventBus;

    @Override
    public void applySkin(Player player, SkinProperty property) {
        SkinApplyEventImpl applyEvent = new SkinApplyEventImpl(player, property);

        eventBus.callEvent(applyEvent);
        if (applyEvent.isCancelled()) {
            return;
        }

        SkinProperty appliedProperty = applyEvent.getProperty();

        player.setGameProfileProperties(updatePropertiesSkin(player.getGameProfileProperties(), appliedProperty));
        wrapper.player(player).sendUpdateRequest(appliedProperty);
    }

    public GameProfile updateProfileSkin(GameProfile profile, SkinProperty property) {
        return new GameProfile(profile.getId(), profile.getName(), updatePropertiesSkin(profile.getProperties(), property));
    }

    private List<Property> updatePropertiesSkin(List<Property> original, SkinProperty property) {
        List<Property> properties = new ArrayList<>(original);

        properties.removeIf(property1 -> property1.getName().equals(SkinProperty.TEXTURES_NAME));
        properties.add(new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));

        return properties;
    }
}
