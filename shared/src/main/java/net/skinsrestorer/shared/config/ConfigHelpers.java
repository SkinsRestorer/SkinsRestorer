package net.skinsrestorer.shared.config;

import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.TypeBasedProperty;
import ch.jalu.configme.properties.types.PrimitivePropertyType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ConfigHelpers {
    public static Property<Integer> newCappedProperty(String path, int defaultValue, int min, int max) {
        return new CappedIntegerProperty(path, defaultValue, min, max);
    }

    public static Property<Locale> newLocaleProperty(String path, Locale defaultValue) {
        return new TypeBasedProperty<>(path, defaultValue, LocaleProperty.instance());
    }

    public static class CappedIntegerProperty extends TypeBasedProperty<Integer> {
        public CappedIntegerProperty(@NotNull String path, @NotNull Integer defaultValue, Integer min, Integer max) {
            super(path, defaultValue, new PrimitivePropertyType<>((object) -> object instanceof Number num ? Math.min(Math.max(num.intValue(), min), max) : null));
        }
    }
}
