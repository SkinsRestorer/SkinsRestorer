/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.floodgate;

import ch.jalu.injector.Injector;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.event.skin.SkinApplyEvent;

import java.util.UUID;

public class FloodgateUtil {
    public static boolean isFloodgateBedrockPlayer(UUID uuid) {
        try {
            boolean isFloodgate = FloodgateApi.getInstance().isFloodgatePlayer(uuid);
            if (!isFloodgate) {
                return false;
            }

            // Linked java accounts should be treated as java players
            return !FloodgateApi.getInstance().getPlayer(uuid).isLinked();
        } catch (Throwable t) {
            // Cancel if Floodgate isn't installed
            return false;
        }
    }

    public static void registerListener(Injector injector) {
        try {
            FloodgateApi.getInstance().getEventBus()
                    .subscribe(SkinApplyEvent.class, injector.getSingleton(FloodgateListener.class));
            System.out.println("Floodgate listener registered");
        } catch (Throwable t) {
            t.printStackTrace();
            // Cancel if Floodgate isn't installed
        }
    }
}
