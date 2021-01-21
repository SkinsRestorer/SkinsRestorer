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

import java.lang.reflect.*;

public class UnsafeImpl implements IAccess {
    protected static final int _FIELD_OFFSET = 24; // Since 1.7
    protected static final int _METHOD_OFFSET = 36; // Since 1.8

    protected DuckReflect _reflect;

    private boolean _canGetValue = false;
    private boolean _canSetValue = false;
    private boolean _canSetFieldModifiers = false;
    private boolean _canSetMethodModifiers = false;

    private boolean _canPrepareField = false;
    private boolean _canAccessObject = false;
    private boolean _canSetModifiers = false;

    private boolean _classExists = false;
    private boolean _instanceExists = false;

    private Class<?> _unsafeClass = null;
    private Object _unsafe = null;

    private Method _staticFieldBase = null;
    private Method _staticFieldOffset = null;
    private Method _objectFieldOffset = null;

    private Method _getObject = null;
    private Method _putObject = null;

    private Method _getAndSetInt = null;

    public UnsafeImpl(DuckReflect reflect, boolean canAccessObject, boolean canSetModifiers) {
        this._reflect = reflect;

        if (canAccessObject) {
            changeGetValue(true);
            changeSetValue(true);
        }

        if (canSetModifiers) {
            changeSetFieldModifiers(true);
            changeSetMethodModifiers(true);
        }
    }

    public UnsafeImpl(DuckReflect reflect) {
        this(reflect, true, false);
    }

    private void setClass() {
        this._unsafeClass = _reflect.getClass("sun.misc.Unsafe");
        this._classExists = (_unsafeClass != null);
    }

    private void setInstance() {
        if (!_classExists) {
            setClass();
        }

        if (_classExists) {
            this._unsafe = _reflect.newInstance(_unsafeClass);
            if (_unsafe == null) {
                this._unsafe = _reflect.getValue(_unsafeClass, "theUnsafe");
            }

            _instanceExists = (_unsafe != null);
        }
    }

    private void setMethods(boolean canAccessObject, boolean canSetModifiers) {
        if (!_instanceExists) {
            setInstance();
        }

        if (_instanceExists) {
            if (canAccessObject) {
                if (!_canPrepareField) {
                    this._staticFieldBase = _reflect.getMethod(_unsafeClass, "staticFieldBase", Field.class);
                    this._staticFieldOffset = _reflect.getMethod(_unsafeClass, "staticFieldOffset", Field.class);
                    this._objectFieldOffset = _reflect.getMethod(_unsafeClass, "objectFieldOffset", Field.class);

                    this._canPrepareField = (_staticFieldBase != null) & (_staticFieldOffset != null) & (_objectFieldOffset != null);
                }

                if (_canPrepareField) {
                    this._getObject = _reflect.getMethod(_unsafeClass, "getObject", Object.class, long.class);
                    this._putObject = _reflect.getMethod(_unsafeClass, "putObject", Object.class, long.class, Object.class);

                    this._canAccessObject = (_getObject != null) & (_putObject != null);
                }
            }

            if (canSetModifiers) {
                this._getAndSetInt = _reflect.getMethod(_unsafeClass, "getAndSetInt", Object.class, long.class, int.class);
                this._canSetModifiers = (_getAndSetInt != null);
            }
        }
    }

    private Object getFieldBase(Field target, Object obj, boolean isStatic) {
        if (!isStatic) return obj;
        return _reflect.call(_staticFieldBase, _unsafe, target);
    }

    private Long getFieldOffset(Field target, boolean isStatic) {
        if (isStatic) {
            return (Long) _reflect.call(_staticFieldOffset, _unsafe, target);
        } else {
            return (Long) _reflect.call(_objectFieldOffset, _unsafe, target);
        }
    }

    @Override
    public Object getValue(Field target, Object obj) {
        if (!_canGetValue | (target == null)) return null;

        Boolean isStatic = Modifier.isStatic(target.getModifiers());
        Object base = getFieldBase(target, obj, isStatic);
        Long offset = getFieldOffset(target, isStatic);

        if (base == null || !base.getClass().getName().equals("<unknown>"))
            return _reflect.call(_getObject, _unsafe, base, offset);
        else return null;
    }

    @Override
    public boolean setValue(Field target, Object obj, Object value) {
        if (!_canSetValue | (target == null)) return false;

        Boolean isStatic = Modifier.isStatic(target.getModifiers());
        Object base = getFieldBase(target, obj, isStatic);
        long offset = getFieldOffset(target, isStatic);

        if (base == null || !base.getClass().getName().equals("<unknown>"))
            return _reflect.exec(_putObject, _unsafe, base, offset, value);
        else return false;
    }

    private boolean setModifiers(Member target, long offset, int mod) {
        return _reflect.exec(_getAndSetInt, _unsafe, target, offset, mod);
    }

    @Override
    public boolean setModifiers(Field target, int mod) {
        if (!_canSetFieldModifiers) return false;
        return setModifiers(target, _FIELD_OFFSET, mod);
    }

    @Override
    public boolean setModifiers(Method target, int mod) {
        if (!_canSetMethodModifiers) return false;
        return setModifiers(target, _METHOD_OFFSET, mod);
    }

    @Override
    public Object getNewInstance(Constructor<?> target, Object... args) {
        return null;
    }

    @Override
    public boolean changeGetValue(boolean enable) {
        if (enable) {
            if (!_canAccessObject) {
                setMethods(true, false);
            }

            this._canGetValue = _canAccessObject;
            return _canGetValue;
        } else {
            this._canGetValue = false;
            return true;
        }
    }

    @Override
    public boolean changeSetValue(boolean enable) {
        if (enable) {
            if (!_canAccessObject) {
                setMethods(true, false);
            }

            this._canSetValue = _canAccessObject;
            return _canSetValue;
        } else {
            this._canSetValue = false;
            return true;
        }
    }

    @Override
    public boolean changeSetFieldModifiers(boolean enable) {
        if (enable) {
            if (!_canSetModifiers) {
                setMethods(false, true);
            }

            this._canSetFieldModifiers = _canSetModifiers;
            return _canSetFieldModifiers;
        } else {
            this._canSetFieldModifiers = false;
            return true;
        }
    }

    @Override
    public boolean changeSetMethodModifiers(boolean enable) {
        if (enable) {
            if (!_canSetModifiers) {
                setMethods(false, true);
            }

            this._canSetMethodModifiers = _canSetModifiers;
            return _canSetMethodModifiers;
        } else {
            this._canSetMethodModifiers = false;
            return true;
        }
    }

    @Override
    public boolean changeGetNewInstance(boolean enable) {
        return false;
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
        return false;
    }
}
