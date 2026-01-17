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
package net.skinsrestorer.mod.wrapper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.skinsrestorer.shared.subjects.messages.ComponentString;

public class ModComponentHelper {
    private static final Gson GSON = new Gson();

    public static Component deserialize(ComponentString messageJson) {
        return ComponentSerialization.CODEC
                .decode(JsonOps.INSTANCE, GSON.fromJson(messageJson.jsonString(), JsonElement.class))
                .getOrThrow()
                .getFirst();
    }
}
