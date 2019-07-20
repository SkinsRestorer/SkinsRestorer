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

import li.cock.ie.access.*;
import java.lang.reflect.*;

public class DuckBypass {
    private DuckReflect _reflect;
    private IAccess _access;

    private boolean _replaced = false;

    public DuckBypass(DuckReflect reflect, IAccess access, boolean replaceSetAccessible) {
        this._reflect = reflect;
        this._access = access;

        if(replaceSetAccessible) {
            replaceMethod();
        }
    }

    public DuckBypass(DuckReflect reflect, IAccess access) {
        this(reflect, access, false);
    }

    public DuckBypass(IAccess access, boolean debug) {
        this(new DuckReflect(debug), access);
    }

    public DuckBypass(IAccess access) {
        this(access, false);
    }

    public DuckBypass(DuckReflect reflect, boolean replaceSetAccessible) {
        this(reflect, new MultiImpl(reflect), replaceSetAccessible);
    }

    public DuckBypass(DuckReflect reflect) {
        this(reflect, false);
    }

    public DuckBypass(boolean replaceSetAccessible, boolean debug) {
        this(new DuckReflect(debug), replaceSetAccessible);
    }

    public DuckBypass(boolean replaceSetAccessible) {
        this(new DuckReflect(false), replaceSetAccessible);
    }

    public DuckBypass() {
        this(false);
    }

    public void replaceMethod() {
        if(!_replaced) {
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

    public boolean setAccessible(AccessibleObject target) {
        return _reflect.setAccessible(target);
    }

    public Class<?> getClass(Object obj) {
        return _reflect.getClass(obj);
    }

    public Class<?> getClass(String name) {
        return _reflect.getClass(name);
    }

    public <T> Constructor<T> getConstructor(Class<T> type, boolean accessible, Class<?>... argTypes) {
        return _reflect.getConstructor(type, accessible, argTypes);
    }

    public <T> Constructor<T> getConstructor(Class<T> type, Class<?>... argTypes) {
        return _reflect.getConstructor(type, argTypes);
    }

    public Class<?>[] getTypes(Object... values) {
        Class<?>[] argTypes = null;

        if(values != null) {
            argTypes = new Class<?>[values.length];
            for(int i = 0; i < values.length; ++i) {
                argTypes[i] = getClass(values[i]);
            }
        }

        return argTypes;
    }

    public Object newInstance(boolean replace, Constructor<?> target, Object... args) {
        if(replace) replaceMethod();
        return _access.getNewInstance(target, args);
    }

    public Object newInstance(Constructor<?> target, Object... args) {
        return newInstance(true, target, args);
    }

    public <T> T newInstance(boolean replace, Class<T> type, Constructor<T> target, Object... args) {
        try {
            return type.cast(newInstance(replace, target, args));
        } catch(Throwable ex) {
            process(ex);
        }

        return null;
    }

    public <T> T newInstance(Class<T> type, Constructor<T> target, Object... args) {
        return newInstance(true, type, target, args);
    }

    public <T> T newInstance(boolean replace, Class<T> type, Class<?>[] argTypes, Object... argValues) {
        return newInstance(replace, type, getConstructor(type, argTypes), argValues);
    }

    public <T> T newInstance(Class<T> type, Class<?>[] argTypes, Object... argValues) {
        return newInstance(true, type, argTypes, argValues);
    }

    public <T> T newInstance(boolean replace, Class<T> type, Object... argValues) {
        return newInstance(replace, type, getTypes(argValues), argValues);
    }

    public <T> T newInstance(Class<T> type, Object... argValues) {
        return newInstance(true, type, argValues);
    }

    public Field getField(Class<?> type, boolean accessible, String name) {
        return _reflect.getField(type, accessible, name);
    }

    public Field getField(Class<?> type, String name) {
        return _reflect.getField(type, name);
    }

    public Method getMethod(Class<?> type, String name, boolean accessible, Class<?>... argTypes) {
        return _reflect.getMethod(type, name, accessible, argTypes);
    }

    public Method getMethod(Class<?> type, String name, Class<?>... argTypes) {
        return _reflect.getMethod(type, name, argTypes);
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
        } catch(Throwable ex) {
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
        if(target instanceof Field) {
            return setModifiers((Field) target, mod);
        } else if(target instanceof Method) {
            return setModifiers((Method) target, mod);
        }

        return false;
    }

    public boolean addModifier(Member target, int mod) {
        return setModifiers(target, target.getModifiers() | mod);
    }

    public boolean delModifier(Member target, int mod) {
        return setModifiers(target, target.getModifiers() & ~mod);
    }

    public boolean setEditable(Member target) {
        int mod = target.getModifiers();
        if(Modifier.isFinal(mod) && Modifier.isStatic(mod)) {
            return delModifier(target, Modifier.FINAL);
        }

        return true;
    }

    public Object call(Method target, Object obj, Object... argValues) {
        return _reflect.call(target, obj, argValues);
    }

    public Object call(Method target, Object[] argValues) {
        return _reflect.call(target, argValues);
    }

    public Object call(Method target) {
        return _reflect.call(target);
    }

    public Object call(Class<?> type, String name, Class<?>[] argTypes, Object obj, Object... argValues) {
        return _reflect.call(type, name, argTypes, obj, argValues);
    }

    public Object call(Class<?> type, String name, Class<?>[] argTypes, Object[] argValues) {
        return _reflect.call(type, name, argTypes, argValues);
    }

    public Object call(Class<?> type, String name, Object obj) {
        return _reflect.call(type, name, obj);
    }

    public Object call(Class<?> type, String name) {
        return _reflect.call(type, name);
    }

    public boolean exec(Method target, Object obj, Object... argValues) {
        return _reflect.exec(target, obj, argValues);
    }

    public boolean exec(Method target, Object[] argValues) {
        return _reflect.exec(target, argValues);
    }

    public boolean exec(Method target) {
        return _reflect.exec(target);
    }

    public boolean exec(Class<?> type, String name, Class<?>[] argTypes, Object obj, Object... argValues) {
        return _reflect.exec(type, name, argTypes, obj, argValues);
    }

    public boolean exec(Class<?> type, String name, Class<?>[] argTypes, Object[] argValues) {
        return _reflect.exec(type, name, argTypes, argValues);
    }

    public boolean exec(Class<?> type, String name, Object obj) {
        return _reflect.exec(type, name, obj);
    }

    public boolean exec(Class<?> type, String name) {
        return _reflect.exec(type, name);
    }
}