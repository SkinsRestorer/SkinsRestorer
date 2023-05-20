/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
