/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.api.bungeecord.events;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import net.skinsrestorer.api.property.IProperty;
import org.jetbrains.annotations.Nullable;

@Getter
public class SkinApplyBungeeEvent extends Event implements Cancellable {
    @Nullable
    private final ProxiedPlayer who;
    @Setter
    private boolean isCancelled = false;
    @Setter
    private IProperty property;

    public SkinApplyBungeeEvent(@Nullable ProxiedPlayer who, IProperty property) {
        this.who = who;
        this.property = property;
    }
}
