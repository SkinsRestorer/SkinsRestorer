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
package net.skinsrestorer.shared.provider;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderSelector<P extends FeatureProvider> {
    private final Class<P> providerType;
    private final List<P> providers;

    public static <P extends FeatureProvider> Builder<P> builder(Class<P> providerType) {
        return new Builder<>(providerType);
    }

    public P get() {
        for (P provider : providers) {
            return providerType.cast(provider);
        }

        throw new IllegalStateException("No provider available for " + providerType.getSimpleName());
    }

    @RequiredArgsConstructor
    public static class Builder<P extends FeatureProvider> {
        private final Class<P> providerType;
        private final List<P> providers = new ArrayList<>();

        public Builder<P> add(String providerClass) {
            try {
                add(Class.forName(providerClass).asSubclass(providerType));
            } catch (Throwable t) {
                t.printStackTrace(); // TODO: debug log
            }
            return this;
        }

        public Builder<P> add(Class<? extends P> providerClass) {
            try {
                Constructor<? extends P> constructor = providerClass.getConstructor();
                P provider = providerType.cast(constructor.newInstance());
                add(provider);
            } catch (Throwable t) {
                t.printStackTrace(); // TODO: debug log
            }
            return this;
        }

        public Builder<P> add(P provider) {
            if (provider.isAvailable()) {
                providers.add(provider);
            }
            return this;
        }

        public P buildAndGet() {
            return new ProviderSelector<P>(providerType, providers).get();
        }
    }
}
