package net.skinsrestorer.shared.utils.property;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public interface IProperty {
    Object getHandle();

    String getName();

    String getValue();

    String getSignature();

    void setName(String name);

    void setValue(String value);

    void setSignature(String signature);

    default boolean valuesFromJson(JsonObject obj) {
        if (obj.has("properties")) {
            JsonArray properties = obj.getAsJsonArray("properties");
            if (properties.size() > 0) {
                JsonObject propertiesObject = properties.get(0).getAsJsonObject();

                setSignature(propertiesObject.get("signature").getAsString());
                setValue(propertiesObject.get("value").getAsString());

                return true;
            }
        }

        return false;
    }
}
