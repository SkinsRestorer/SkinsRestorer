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
    }

    public static Optional<InputDataResult> getHardcodedSkin(String input) {
        var lowerCaseName = input.toLowerCase(Locale.ROOT);
        return Optional.ofNullable(SKINS.get(lowerCaseName))
                .map(property -> InputDataResult.of(
                        SkinIdentifier.ofCustom(lowerCaseName), property));
    }
}
