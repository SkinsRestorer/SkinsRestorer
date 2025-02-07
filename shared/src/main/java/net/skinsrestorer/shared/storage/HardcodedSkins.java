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
package net.skinsrestorer.shared.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class HardcodedSkins {
    private static final Map<String, SkinProperty> SKINS = new HashMap<>();
    public static final InputDataResult STEVE;
    public static final InputDataResult ALEX;

    static {
        var gson = new Gson();
        try (var in = Objects.requireNonNull(HardcodedSkins.class.getClassLoader().getResourceAsStream("hardcoded_skins.json"));
             var reader = new InputStreamReader(in)) {
            var map = gson.fromJson(reader, JsonObject.class);
            for (var entry : map.entrySet()) {
                var name = entry.getKey().toLowerCase(Locale.ROOT);
                var property = entry.getValue().getAsJsonObject();
                var value = property.get("value").getAsString();
                var signature = property.get("signature").getAsString();

                SKINS.put(name, SkinProperty.of(value, signature));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        STEVE = getHardcodedSkin("steve").orElseThrow();
        ALEX = getHardcodedSkin("alex").orElseThrow();
    }

    public static Optional<InputDataResult> getHardcodedSkin(String input) {
        var lowerCaseName = input.toLowerCase(Locale.ROOT);
        return Optional.ofNullable(SKINS.get(lowerCaseName))
                .map(property -> InputDataResult.of(
                        SkinIdentifier.ofCustom(lowerCaseName), property));
    }
}
