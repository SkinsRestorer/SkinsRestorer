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
package net.skinsrestorer.shared.storage.adapter;

import lombok.Setter;

import javax.inject.Provider;
import java.util.Objects;
import java.util.function.Function;

@Setter
public class AdapterReference implements Provider<StorageAdapter> {
    private StorageAdapter adapter;

    @Override
    public StorageAdapter get() {
        return Objects.requireNonNull(adapter, "We're not connected to a storage backend!");
    }

    public <T> T getOrDefault(Function<StorageAdapter, T> function, T defaultValue) {
        if (adapter == null) {
            return defaultValue;
        }
        return function.apply(adapter);
    }

    public <T, E extends Throwable> T getOrDefaultThrowing(ThrowingFunction<StorageAdapter, T, E> function, T defaultValue) throws E {
        if (adapter == null) {
            return defaultValue;
        }
        return function.apply(adapter);
    }

    @FunctionalInterface
    public interface ThrowingFunction<I, O, E extends Throwable> {
        O apply(I input) throws E;
    }
}
