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
package net.skinsrestorer.shared.provider;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderSelector<P extends FeatureProvider> {
    private final List<P> providers;

    public static <P extends FeatureProvider> Builder<P> builder() {
        return new Builder<>();
    }

    public P get() {
        for (P provider : providers) {
            return provider;
        }

        throw new IllegalStateException("No provider available.");
    }

    @RequiredArgsConstructor
    public static class Builder<P extends FeatureProvider> {
        private final List<P> providers = new ArrayList<>();

        public Builder<P> add(P provider) {
            if (provider.isAvailable()) {
                providers.add(provider);
            }
            return this;
        }

        public P buildAndGet() {
            return new ProviderSelector<P>(providers).get();
        }
    }
}
