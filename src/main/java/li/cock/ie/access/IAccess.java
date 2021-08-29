/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
package li.cock.ie.access;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface IAccess {
    Object getValue(Field target, Object obj);

    boolean setValue(Field target, Object obj, Object value);

    boolean setModifiers(Field target, int mod);

    boolean setModifiers(Method target, int mod);

    Object getNewInstance(Constructor<?> target, Object... args);

    boolean changeGetValue(boolean enable);

    boolean changeSetValue(boolean enable);

    boolean changeSetFieldModifiers(boolean enable);

    boolean changeSetMethodModifiers(boolean enable);

    boolean changeGetNewInstance(boolean enable);

}