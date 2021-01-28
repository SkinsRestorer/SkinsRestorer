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
package net.skinsrestorer.shared.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;

import java.io.Serializable;

public @Data class Property implements Serializable {
    private String name;
    private String value;
    private String signature;

    public boolean valuesFromJson(JsonObject obj) {
        if (obj.has("properties")) {
            JsonArray properties = obj.getAsJsonArray("properties");
            if (properties.size() > 0) {
                JsonObject propertiesObject = properties.get(0).getAsJsonObject();

                signature = propertiesObject.get("signature").getAsString();
                value = propertiesObject.get("value").getAsString();

                return true;
            }
        }

        return false;
    }
}
