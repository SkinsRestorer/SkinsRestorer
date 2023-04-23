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
package net.skinsrestorer.velocity.listener;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ForceAliveListener implements EventHandler<ProxyShutdownEvent> {
    @SuppressWarnings("unused")
    private final Object object; // Object to keep alive

    @Override
    public void execute(ProxyShutdownEvent event) {
        // NO-OP
    }
}
