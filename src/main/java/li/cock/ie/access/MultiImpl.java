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

package li.cock.ie.access;

import li.cock.ie.reflect.DuckReflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class MultiImpl implements IAccess {
    protected DuckReflect _reflect;
    protected List<IAccess> _access = new ArrayList<IAccess>();

    private boolean _canGetValue;
    private boolean _canSetValue;
    private boolean _canSetFieldModifiers;
    private boolean _canSetMethodModifiers;
    private boolean _canGetNewInstance;

    private boolean _preferUnsafe;

    public MultiImpl(DuckReflect reflect, boolean preferUnsafe, int getValDamage, int setValDamage, int setModifiersDamage, int newInstanceDamage) {
        this._reflect = reflect;
        this._preferUnsafe = preferUnsafe;

        this._canGetValue = (getValDamage > 0);
        this._canSetValue = (setValDamage > 0);
        this._canSetFieldModifiers = (setModifiersDamage > 0);
        this._canSetMethodModifiers = (setModifiersDamage > 0);
        this._canGetNewInstance = (newInstanceDamage > 0);

        DefaultImpl defImpl = new DefaultImpl(reflect, (setModifiersDamage > 0));
        ReflectImpl refImpl = new ReflectImpl(reflect, false, (newInstanceDamage > 1), (getValDamage < 2 & setValDamage < 2), (getValDamage > 2 | setValDamage > 2));
        UnsafeImpl unsImpl = new UnsafeImpl(reflect, (getValDamage > 2 | setValDamage > 2), (setModifiersDamage > 1));

        if (getValDamage < 1) defImpl.changeGetValue(false);
        if (getValDamage < 2) refImpl.changeGetValue(false);
        if (getValDamage < 3) unsImpl.changeGetValue(false);

        if (setValDamage < 1) defImpl.changeSetValue(false);
        if (setValDamage < 2) refImpl.changeSetValue(false);
        if (setValDamage < 3) unsImpl.changeSetValue(false);

        if (newInstanceDamage < 1) defImpl.changeGetNewInstance(false);

        _access.add(defImpl);
        _access.add(refImpl);
        _access.add(unsImpl);
    }

    public MultiImpl(DuckReflect reflect, boolean preferUnsafe, int accessDamage, int setModifiersDamage, int newInstanceDamage) {
        this(reflect, preferUnsafe, accessDamage / 2 + accessDamage % 2, accessDamage / 2, setModifiersDamage, newInstanceDamage);
    }

    public MultiImpl(DuckReflect reflect, boolean preferUnsafe, int refDamage, int newInstanceDamage) {
        this(reflect, preferUnsafe, refDamage - refDamage / 5, refDamage / 5 + refDamage / 8, newInstanceDamage);
    }

    public MultiImpl(DuckReflect reflect, boolean preferUnsafe) {
        this(reflect, preferUnsafe, 8, 2);
    }

    public MultiImpl(DuckReflect reflect, int refDamage, int newInstanceDamage) {
        this(reflect, true, refDamage, newInstanceDamage);
    }

    public MultiImpl(DuckReflect reflect) {
        this(reflect, 8, 2);
    }

    public void add(IAccess access) {
        _access.add(access);
    }

    @Override
    public Object getValue(Field target, Object obj) {
        if (!_canGetValue) return null;

        _reflect.reset();

        for (IAccess access : _access) {
            Object res = access.getValue(target, obj);
            if (res != null & _reflect.check()) {
                return res;
            }
        }

        return null;
    }

    private boolean defSetValue(Field target, Object obj, Object value) {
        for (IAccess access : _access) {
            if (access.setValue(target, obj, value) & _reflect.check()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean setValue(Field target, Object obj, Object value) {
        if (!_canSetValue | (target == null)) return false;

        _reflect.reset();

        int mod = target.getModifiers();
        if (Modifier.isFinal(mod) && Modifier.isStatic(mod)) {
            if (!_preferUnsafe) {
                if (_access.get(0).setModifiers(target, mod & ~Modifier.FINAL) & _reflect.check()) {
                    return defSetValue(target, obj, value);
                } else if (_access.get(2).setValue(target, obj, value) & _reflect.check()) {
                    return true;
                } else if (_access.get(2).setModifiers(target, mod & ~Modifier.FINAL) & _reflect.check()) {
                    return defSetValue(target, obj, value);
                } else {
                    return false;
                }
            } else {
                if (_access.get(2).setValue(target, obj, value) & _reflect.check()) {
                    return true;
                } else if (_access.get(0).setModifiers(target, mod & ~Modifier.FINAL) & _reflect.check()) {
                    return defSetValue(target, obj, value);
                } else if (_access.get(2).setModifiers(target, mod & ~Modifier.FINAL) & _reflect.check()) {
                    return defSetValue(target, obj, value);
                } else {
                    return false;
                }
            }
        } else {
            return defSetValue(target, obj, value);
        }
    }

    @Override
    public boolean setModifiers(Field target, int mod) {
        if (!_canSetFieldModifiers) return false;

        _reflect.reset();

        for (IAccess access : _access) {
            if (access.setModifiers(target, mod) & _reflect.check()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean setModifiers(Method target, int mod) {
        if (!_canSetMethodModifiers) return false;

        _reflect.reset();

        for (IAccess access : _access) {
            if (access.setModifiers(target, mod) & _reflect.check()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object getNewInstance(Constructor<?> target, Object... args) {
        if (!_canGetNewInstance) return null;

        _reflect.reset();

        for (IAccess access : _access) {
            Object res = access.getNewInstance(target, args);
            if (res != null & _reflect.check()) {
                return res;
            }
        }

        return null;
    }

    @Override
    public boolean changeGetValue(boolean enable) {
        this._canGetValue = enable;
        return true;
    }

    @Override
    public boolean changeSetValue(boolean enable) {
        this._canSetValue = enable;
        return true;
    }

    @Override
    public boolean changeSetFieldModifiers(boolean enable) {
        this._canSetFieldModifiers = enable;
        return true;
    }

    @Override
    public boolean changeSetMethodModifiers(boolean enable) {
        this._canSetMethodModifiers = enable;
        return true;
    }

    @Override
    public boolean changeGetNewInstance(boolean enable) {
        this._canGetNewInstance = enable;
        return true;
    }

    @Override
    public boolean canGetValue() {
        return _canGetValue;
    }

    @Override
    public boolean canSetValue() {
        return _canSetValue;
    }

    @Override
    public boolean canSetFieldModifiers() {
        return _canSetFieldModifiers;
    }

    @Override
    public boolean canSetMethodModifiers() {
        return _canSetMethodModifiers;
    }

    @Override
    public boolean canGetNewInstance() {
        return _canGetNewInstance;
    }
}
