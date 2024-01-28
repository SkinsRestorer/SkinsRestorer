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
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderSelector<P extends FeatureProvider> {
    public static <P extends FeatureProvider> Builder<P> builder() {
        return new Builder<>();
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder<P extends FeatureProvider> {
        private P provider;

        public Builder<P> add(P provider) {
            if (this.provider != null) {
                return this;
            }

            if (provider.isAvailable()) {
                this.provider = provider;
            }

            return this;
        }

        public P get() {
            return Objects.requireNonNull(provider, "No provider available");
        }
    }
}
