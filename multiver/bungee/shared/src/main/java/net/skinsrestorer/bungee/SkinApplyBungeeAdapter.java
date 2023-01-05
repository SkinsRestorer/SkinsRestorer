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
package net.skinsrestorer.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.reflection.exception.ReflectionException;

import java.util.List;

public interface SkinApplyBungeeAdapter {
    void applyToHandler(InitialHandler handler, SkinProperty textures) throws ReflectionException;

    List<SkinProperty> getProperties(ProxiedPlayer player);
}
