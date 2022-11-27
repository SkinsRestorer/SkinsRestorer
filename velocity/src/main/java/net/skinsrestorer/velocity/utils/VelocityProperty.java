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
package net.skinsrestorer.velocity.utils;

import com.velocitypowered.api.util.GameProfile;
import lombok.ToString;
import net.skinsrestorer.api.property.IProperty;

@ToString
public class VelocityProperty implements IProperty {
    private final GameProfile.Property property;

    public VelocityProperty(String name, String value, String signature) {
        this(new GameProfile.Property(name, value, signature));
    }

    public VelocityProperty(GameProfile.Property property) {
        this.property = property;
    }

    @Override
    public Object getHandle() {
        return property;
    }

    @Override
    public String getName() {
        return property.getName();
    }

    @Override
    public String getValue() {
        return property.getValue();
    }

    @Override
    public String getSignature() {
        return property.getSignature();
    }
}
