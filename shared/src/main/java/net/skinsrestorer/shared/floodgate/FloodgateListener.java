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
package net.skinsrestorer.shared.floodgate;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.shared.log.SRLogger;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Consumer;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FloodgateListener implements Consumer<SkinApplyEvent> {
    private final SRLogger logger;
    private final PlayerStorage playerStorage;

    @Override
    public void accept(SkinApplyEvent event) {
        FloodgatePlayer floodgatePlayer = event.player();
        logger.debug("Handling Floodgate skin apply for %s (%s)".formatted(floodgatePlayer.getCorrectUsername(), floodgatePlayer.getCorrectUniqueId()));
        try {
            Optional<SkinProperty> optional =
                    playerStorage.getSkinForPlayer(floodgatePlayer.getCorrectUniqueId(), floodgatePlayer.getCorrectUsername(), true);

            optional.ifPresent(skinProperty ->
                    event.newSkin(new SkinDataImpl(skinProperty.getValue(), skinProperty.getSignature())));
        } catch (DataRequestException e) {
            logger.warning("Failed to get skin for %s (%s)".formatted(floodgatePlayer.getCorrectUsername(), floodgatePlayer.getCorrectUniqueId()), e);
        }
    }

    private record SkinDataImpl(String value, String signature) implements SkinApplyEvent.SkinData {
    }
}
