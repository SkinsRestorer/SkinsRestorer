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
package net.skinsrestorer.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.inject.Provider;
import java.util.Objects;
import java.util.function.BooleanSupplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderSelector<P> {
    private P provider;

    public static <P> ProviderSelector<P> selector() {
        return new ProviderSelector<>();
    }

    public ProviderSelector<P> add(BooleanSupplier availableCheck, Provider<P> provider) {
        if (this.provider != null) {
            return this;
        }

        if (availableCheck.getAsBoolean()) {
            this.provider = provider.get();
        }

        return this;
    }

    public ProviderSelector<P> add(BooleanSupplier availableCheck, P provider) {
        return add(availableCheck, () -> provider);
    }

    public ProviderSelector<P> addDefault(Provider<P> provider) {
        return add(() -> true, provider);
    }

    public ProviderSelector<P> addDefault(P provider) {
        return addDefault(() -> provider);
    }

    public P get() {
        return Objects.requireNonNull(provider, "No provider available");
    }
}
