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
package net.skinsrestorer.api.velocity.events;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.skinsrestorer.api.property.SkinProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class SkinApplyVelocityEvent implements ResultedEvent<ResultedEvent.GenericResult> {
    private final Player who;
    @Setter
    private SkinProperty property;
    @NonNull
    private GenericResult result = GenericResult.allowed();

    public SkinApplyVelocityEvent(@Nullable Player who, SkinProperty property) {
        this.who = who;
        this.property = property;
    }

    public GenericResult result() {
        return result;
    }

    @Override
    public void setResult(GenericResult result) {
        this.result = Objects.requireNonNull(result);
    }
}
