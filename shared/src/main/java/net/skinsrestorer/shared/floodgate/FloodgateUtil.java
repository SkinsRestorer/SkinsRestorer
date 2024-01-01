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

import ch.jalu.injector.Injector;
import net.skinsrestorer.shared.log.SRLogger;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;

import java.util.UUID;

public class FloodgateUtil {
    public static boolean isFloodgateBedrockPlayer(UUID uuid) {
        try {
            return FloodgateApi.getInstance().isFloodgateId(uuid);
        } catch (Throwable t) {
            // Cancel if Floodgate isn't installed
            return false;
        }
    }

    public static void registerListener(Injector injector) {
        SRLogger logger = injector.getSingleton(SRLogger.class);
        try {
            FloodgateApi.getInstance().getEventBus()
                    .subscribe(SkinApplyEvent.class, injector.getSingleton(FloodgateListener.class));
            logger.info("Floodgate skin listener registered");
        } catch (Throwable t) {
            logger.severe("Failed to register Floodgate skin listener (Is floodgate and SkinsRestorer up to date?)", t);
        }
    }
}
