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
package net.skinsrestorer.api.event;

import java.util.function.Consumer;

/**
 * The SkinsRestorerEvent interface represents an event that can be triggered
 * by the SkinsRestorer plugin. All event classes that are part of the
 * SkinsRestorer event system should implement this interface.
 *
 * <p>
 * To listen for SkinsRestorer events, you can create a class that implements
 * the corresponding event listener interface, and register your listener using
 * the {@link EventBus#subscribe(Object, Class, Consumer)} method.
 * </p>
 *
 * @see EventBus
 */
public interface SkinsRestorerEvent {
}
