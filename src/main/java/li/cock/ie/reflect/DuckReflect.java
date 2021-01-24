/*
 * Copyright 2019 Ilya Egorov <ie9@null.net>. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package li.cock.ie.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DuckReflect {
    protected IDuckHandler _handler;
    private Method _setAccessible;

    public DuckReflect(IDuckHandler handler) {
        this._handler = handler;

        try {
            this._setAccessible = AccessibleObject.class.getMethod("setAccessible", boolean.class);
            _setAccessible.setAccessible(true);
        } catch (Exception ex) {
            process(ex);
        }
    }

    public DuckReflect(boolean debug) {
        this(new DuckHandler(debug));
    }

    public DuckReflect() {
        this(false);
    }

    public void reset() {
        _handler.reset();
    }

    public boolean check() {
        return _handler.check();
    }

    public void process(Throwable ex) {
        _handler.process(ex);
    }

    public boolean setAccessible(AccessibleObject target) {
        if (_setAccessible == null || target == null) return false;

        try {
            _setAccessible.invoke(target, true);
            return true;
        } catch (Throwable ex) {
            process(ex);
        }

        return false;
    }

    public boolean replaceMethod(Method setAccessible) {
        if (setAccessible == null) return false;

        this._setAccessible = setAccessible;
        return true;
    }

    public Class<?> getClass(Object obj) {
        if (obj == null) return null;

        try {
            return obj.getClass();
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public <T> Constructor<T> getConstructor(Class<T> type, boolean accessible, Class<?>... argTypes) {
        if (type == null) return null;

        try {
            Constructor<T> constructor = type.getDeclaredConstructor(argTypes);
            if (accessible) {
                setAccessible(constructor);
            }

            return constructor;
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public <T> Constructor<T> getConstructor(Class<T> type, Class<?>... argTypes) {
        return getConstructor(type, true, argTypes);
    }

    public <T> T newInstance(Constructor<T> target, Object... args) {
        if (target == null) return null;

        try {
            return target.newInstance(args);
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public <T> T newInstance(Class<T> type, Class<?>[] argTypes, Object... argValues) {
        return newInstance(getConstructor(type, argTypes), argValues);
    }

    public <T> T newInstance(Class<T> type) {
        return newInstance(getConstructor(type, null), null);
    }

    public Field getField(Class<?> type, boolean accessible, String name) {
        if (type == null) return null;

        try {
            Field field = type.getDeclaredField(name);
            if (accessible) {
                setAccessible(field);
            }

            return field;
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public Field getField(Class<?> type, String name) {
        return getField(type, true, name);
    }

    public Method getMethod(Class<?> type, String name, boolean accessible, Class<?>... argTypes) {
        if (type == null) return null;

        try {
            Method method = type.getDeclaredMethod(name, argTypes);
            if (accessible) {
                setAccessible(method);
            }

            return method;
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public Method getMethod(Class<?> type, String name, Class<?>... argTypes) {
        return getMethod(type, name, true, argTypes);
    }

    public Object getValue(Field target, Object obj) {
        if (target == null) return null;

        try {
            return target.get(obj);
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
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

    public boolean setValue(Field target, Object obj, Object value) {
        if (target == null) return false;

        try {
            target.set(obj, value);
            return true;
        } catch (Throwable ex) {
            process(ex);
        }

        return false;
    }

    public boolean setValue(Field target, Object value) {
        return setValue(target, null, value);
    }

    public boolean setValue(Class<?> type, String name, Object obj, Object value) {
        return setValue(getField(type, name), obj, value);
    }

    public boolean setValue(Class<?> type, String name, Object value) {
        return setValue(type, name, null, value);
    }

    public Object call(Method target, Object obj, Object... argValues) {
        if (target == null) return null;

        try {
            return target.invoke(obj, argValues);
        } catch (Throwable ex) {
            process(ex);
        }

        return null;
    }

    public Object call(Method target, Object[] argValues) {
        return call(target, null, argValues);
    }

    public Object call(Method target) {
        return call(target, null);
    }

    public Object call(Class<?> type, String name, Class<?>[] argTypes, Object obj, Object... argValues) {
        return call(getMethod(type, name, argTypes), obj, argValues);
    }

    public Object call(Class<?> type, String name, Class<?>[] argTypes, Object[] argValues) {
        return call(type, name, argTypes, null, argValues);
    }

    public Object call(Class<?> type, String name, Object obj) {
        return call(type, name, null, obj);
    }

    public Object call(Class<?> type, String name) {
        return call(type, name, null);
    }

    public boolean exec(Method target, Object obj, Object... argValues) {
        if (target == null) return false;

        try {
            target.invoke(obj, argValues);
            return true;
        } catch (Throwable ex) {
            process(ex);
        }

        return false;
    }

    public boolean exec(Method target, Object[] argValues) {
        return exec(target, null, argValues);
    }

    public boolean exec(Method target) {
        return exec(target, null);
    }

    public boolean exec(Class<?> type, String name, Class<?>[] argTypes, Object obj, Object... argValues) {
        return exec(getMethod(type, name, argTypes), obj, argValues);
    }

    public boolean exec(Class<?> type, String name, Class<?>[] argTypes, Object[] argValues) {
        return exec(type, name, argTypes, null, argValues);
    }

    public boolean exec(Class<?> type, String name, Object obj) {
        return exec(type, name, null, obj);
    }

    public boolean exec(Class<?> type, String name) {
        return exec(type, name, null);
    }
}
