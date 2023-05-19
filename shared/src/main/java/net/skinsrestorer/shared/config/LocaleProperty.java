package net.skinsrestorer.shared.config;

import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import lombok.NoArgsConstructor;
import net.skinsrestorer.shared.utils.LocaleParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class LocaleProperty implements PropertyType<Locale> {
    @Override
    public @Nullable Locale convert(@Nullable Object object, @NotNull ConvertErrorRecorder errorRecorder) {
        return LocaleParser.parseLocale(object == null ? null : object.toString()).orElse(null);
    }

    @Override
    public @NotNull Object toExportValue(Locale value) {
        return value.toString();
    }

    public static LocaleProperty instance() {
        return new LocaleProperty();
    }
}
