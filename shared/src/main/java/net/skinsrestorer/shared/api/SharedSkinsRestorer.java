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
package net.skinsrestorer.shared.api;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.event.EventBus;
import net.skinsrestorer.api.interfaces.MineSkinAPI;
import net.skinsrestorer.api.interfaces.MojangAPI;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.api.interfaces.SkinStorage;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Base64;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedSkinsRestorer implements SkinsRestorer {
    @Getter
    private final SkinStorage skinStorage;
    @Getter
    private final MojangAPI mojangAPI;
    @Getter
    private final MineSkinAPI mineSkinAPI;
    private final SharedSkinApplier<?> skinApplier;
    private final EventBusImpl eventBus;
    private final Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    @Override
    public <P> SkinApplier<P> getSkinApplier(Class<P> playerClass) {
        if (!skinApplier.accepts(playerClass)) {
            throw new IllegalArgumentException("Unsupported player class: " + playerClass.getName());
        }

        return (SkinApplier<P>) skinApplier;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public MojangProfileResponse getSkinProfileData(@NotNull SkinProperty property)  {
        String decodedString = new String(Base64.getDecoder().decode(property.getValue()));

        return gson.fromJson(decodedString, MojangProfileResponse.class);
    }
}
