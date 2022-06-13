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

import lombok.ToString;
import net.md_5.bungee.connection.LoginResult.Property;
import net.skinsrestorer.api.property.IProperty;

@ToString
public class BungeePropertyOld implements IProperty {
    private final Property property;

    public BungeePropertyOld(String name, String value, String signature) {
        this(new Property(name, value, signature));
    }

    public BungeePropertyOld(Property property) {
        this.property = property;
    }

    @Override
    public Object getHandle() {
        return this;
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
