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
package li.cock.ie.reflect;

import li.cock.ie.access.IAccess;
import li.cock.ie.access.MultiImpl;

import java.lang.reflect.*;

public class DuckBypass {
    private final DuckReflect _reflect;
    private final IAccess _access;

    private boolean _replaced = false;

    public DuckBypass(DuckReflect reflect, IAccess access, boolean replaceSetAccessible) {
        this._reflect = reflect;
        this._access = access;

        if (replaceSetAccessible) {
            replaceMethod();
        }
    }

    public DuckBypass(DuckReflect reflect, boolean replaceSetAccessible) {
        this(reflect, new MultiImpl(reflect), replaceSetAccessible);
    }

    public DuckBypass(boolean replaceSetAccessible) {
        this(new DuckReflect(false), replaceSetAccessible);
    }

    public DuckBypass() {
        this(false);
    }

    public void replaceMethod() {
        if (!_replaced) {
            Method setAccessible = _reflect.getMethod(AccessibleObject.class, "setAccessible0", false, boolean.class);
            if (setAccessible != null) {
                if (_access.setModifiers(setAccessible, Modifier.PUBLIC)) {
                    _reflect.replaceMethod(setAccessible);
                } else if (_reflect.setAccessible(setAccessible)) {
                    _reflect.replaceMethod(setAccessible);
                }
            }

            this._replaced = true;
        }
    }

    public void reset() {
        _reflect.reset();
    }

    public boolean check() {
        return _reflect.check();
    }

    public void process(Throwable ex) {
        _reflect.process(ex);
    }

    public Class<?> getClass(Object obj) {
        return _reflect.getClass(obj);
    }

    public Class<?> getClass(String name) {
        return _reflect.getClass(name);
    }

    public Field getField(Class<?> type, String name) {
        return _reflect.getField(type, name);
    }

    public Object getValue(Field target, Object obj) {
        return _access.getValue(target, obj);
    }

    public Object getValue(Field target) {
        return getValue(target, null);
    }

    public Object getValue(Class<?> type, String name, Object obj) {
        return getValue(getField(type, name), obj);
    }

    public Object getValue(Class<?> type, String name) {
        return getValue(type, name, null);
    }

    public <T> T getValue(Field target, Object obj, Class<T> valType) {
        try {
            return valType.cast(getValue(target, obj));
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public <T> T getValue(Field target, Class<T> valType) {
        return getValue(target, null, valType);
    }

    public <T> T getValue(Class<?> type, String name, Object obj, Class<T> valType) {
        return getValue(getField(type, name), obj, valType);
    }

    public <T> T getValue(Class<?> type, String name, Class<T> valType) {
        return getValue(type, name, null, valType);
    }

    public boolean setValue(Field target, Object obj, Object value) {
        return _access.setValue(target, obj, value);
    }

    public boolean setValue(Field target, Object value) {
        return setValue(target, null, value);
    }

    public boolean setValue(Field target) {
        return setValue(target, null);
    }

    public boolean setValue(Class<?> type, String name, Object obj, Object value) {
        return setValue(getField(type, name), obj, value);
    }

    public boolean setValue(Class<?> type, String name, Object value) {
        return setValue(type, name, null, value);
    }

    public boolean setValue(Class<?> type, String name) {
        return setValue(type, name, null);
    }

    public boolean setModifiers(Field target, int mod) {
        return _access.setModifiers(target, mod);
    }

    public boolean setModifiers(Method target, int mod) {
        return _access.setModifiers(target, mod);
    }

    public boolean setModifiers(Member target, int mod) {
        if (target instanceof Field) {
            return setModifiers((Field) target, mod);
        } else if (target instanceof Method) {
            return setModifiers((Method) target, mod);
        }

        return false;
    }

    public boolean delModifier(Member target, int mod) {
        return setModifiers(target, target.getModifiers() & ~mod);
    }

    public boolean setEditable(Member target) {
        int mod = target.getModifiers();
        if (Modifier.isFinal(mod) && Modifier.isStatic(mod)) {
            return delModifier(target, Modifier.FINAL);
        }

        return true;
    }
}