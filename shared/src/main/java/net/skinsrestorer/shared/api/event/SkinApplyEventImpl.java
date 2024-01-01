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
package net.skinsrestorer.shared.api.event;

import lombok.Getter;
import lombok.Setter;
import net.skinsrestorer.api.event.SkinApplyEvent;
import net.skinsrestorer.api.property.SkinProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class SkinApplyEventImpl implements SkinApplyEvent {
    @Nullable
    private final Object player;
    private SkinProperty property;
    @Setter
    private boolean cancelled;

    public SkinApplyEventImpl(@Nullable Object player, SkinProperty property) {
        this.player = player;
        this.property = property;
    }

    @Override
    public <P> P getPlayer(Class<P> playerClass) {
        return playerClass.cast(player);
    }

    public void setProperty(SkinProperty property) {
        Objects.requireNonNull(property, "property");
        this.property = property;
    }
}
