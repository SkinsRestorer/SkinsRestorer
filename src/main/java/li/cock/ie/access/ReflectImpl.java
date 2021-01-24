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

public class ReflectImpl extends DefaultImpl {
    private boolean _canGetValue = true;
    private boolean _canSetValue = true;
    private boolean _canGetNewInstance = false;

    private boolean _useDefaultAccess = true;
    private boolean _canUnsafeAccess;

    private boolean _classesExist = false;
    private boolean _newInstancesExist = false;
    private boolean _methodsExist = false;
    private boolean _lazy;

    private Class<?> _reflectionFactory = null;
    private Class<?> _fieldAccessor = null;

    private Object _factory = null;

    private Method _newFieldAccessor = null;

    private Method _set = null;
    private Method _get = null;

    private Method _acquireConstructorAccessor = null;
    private Method _newConstructorAccessor = null;

    private Method _newInstance = null;

    private Field _constructorAccessor = null;

    public ReflectImpl(DuckReflect reflect, boolean canSetModifiers, boolean canGetNewInstance, boolean useDefaultAccess, boolean canUnsafeAccess, boolean lazy) {
        super(reflect, canSetModifiers);

        this._lazy = (canGetNewInstance & lazy);
        this._canUnsafeAccess = canUnsafeAccess;

        if (!useDefaultAccess) {
            useDefaultAccess(false);
        }

        if (canGetNewInstance & !_lazy) {
            changeGetNewInstance(true);
        }
    }

    public ReflectImpl(DuckReflect reflect, boolean canSetModifiers, boolean canGetNewInstance, boolean useDefaultAccess, boolean canUnsafeAccess) {
        this(reflect, canSetModifiers, canGetNewInstance, useDefaultAccess, canUnsafeAccess, true);
    }

    public ReflectImpl(DuckReflect reflect, boolean canSetModifiers, boolean canGetNewInstance, boolean useDefaultAccess) {
        this(reflect, canSetModifiers, canGetNewInstance, useDefaultAccess, false);
    }

    public ReflectImpl(DuckReflect reflect, boolean canSetModifiers, boolean canGetNewInstance) {
        this(reflect, canSetModifiers, canGetNewInstance, false);
    }

    public ReflectImpl(DuckReflect reflect) {
        this(reflect, true, true);
    }

    private void setClasses() {
        this._reflectionFactory = _reflect.getClass("sun.reflect.ReflectionFactory");
        this._fieldAccessor = _reflect.getClass("sun.reflect.FieldAccessor");

        this._classesExist = (_reflectionFactory != null) & (_fieldAccessor != null);
    }

    private void setNewInstances() {
        if (!_classesExist) {
            setClasses();
        }

        if (_classesExist) {
            this._newFieldAccessor = _reflect.getMethod(_reflectionFactory, "newFieldAccessor", Field.class, boolean.class);
            this._factory = _reflect.call(_reflectionFactory, "getReflectionFactory");

            this._newInstancesExist = (_newFieldAccessor != null) && (_factory != null);
        }
    }

    private void setMethods() {
        if (!_newInstancesExist) {
            setNewInstances();
        }

        if (_newInstancesExist) {
            this._get = _reflect.getMethod(_fieldAccessor, "get", Object.class);
            this._set = _reflect.getMethod(_fieldAccessor, "set", Object.class, Object.class);

            this._methodsExist = (_get != null) & (_set != null);
        }
    }

    @Override
    public Object getValue(Field target, Object obj) {
        if (!_canGetValue) return null;
        if (_useDefaultAccess) {
            return super.getValue(target, obj);
        }

        Object fieldAccessor = _reflect.call(_newFieldAccessor, _factory, target, _canUnsafeAccess);
        return _reflect.call(_get, fieldAccessor, obj);
    }

    @Override
    public boolean setValue(Field target, Object obj, Object value) {
        if (!_canSetValue) return false;
        if (_useDefaultAccess) {
            return super.setValue(target, obj, value);
        }

        Object fieldAccessor = _reflect.call(_newFieldAccessor, _factory, target, _canUnsafeAccess);
        return _reflect.exec(_set, fieldAccessor, obj, value);
    }

    private Object getConstructorAccessor(Constructor<?> target) {
        if (_constructorAccessor != null) {
            Object accessor = _reflect.getValue(_constructorAccessor, target);
            if (accessor != null) return accessor;
        }

        _reflect.reset();

        if (_acquireConstructorAccessor != null) {
            Object accessor = _reflect.call(_acquireConstructorAccessor, target);
            if (accessor != null) return accessor;
        }

        _reflect.reset();

        return _reflect.call(_newConstructorAccessor, _factory, target);
    }

    @Override
    public Object getNewInstance(Constructor<?> target, Object... args) {
        if (!_canGetNewInstance) {
            if (_lazy) {
                changeGetNewInstance(true);
                this._lazy = false;

                _reflect.reset();
            } else {
                return null;
            }
        }

        Object accessor = getConstructorAccessor(target);

        if (this._newInstance == null & accessor != null) {
            this._newInstance = _reflect.getMethod(accessor.getClass(), "newInstance", Object[].class);
        }

        return _reflect.call(_newInstance, accessor, new Object[]{args});
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
    public boolean changeGetNewInstance(boolean enable) {
        if (enable) {
            if (_constructorAccessor == null & _acquireConstructorAccessor == null & _newConstructorAccessor == null & _newInstance == null) {
                this._constructorAccessor = _reflect.getField(Constructor.class, "constructorAccessor");
                this._acquireConstructorAccessor = _reflect.getMethod(Constructor.class, "acquireConstructorAccessor");
                this._newConstructorAccessor = _reflect.getMethod(_reflectionFactory, "newConstructorAccessor", Constructor.class);

                this._newInstance = _reflect.getMethod(_reflect.getClass("sun.reflect.ConstructorAccessor"), "newInstance", Object[].class);
            }

            this._canGetNewInstance = (_constructorAccessor != null) | (_acquireConstructorAccessor != null) | (_newConstructorAccessor != null);
            return _canGetNewInstance;
        } else {
            this._canGetNewInstance = true;
            return true;
        }
    }

    public boolean useDefaultAccess(boolean enable) {
        if (!enable) {
            if (!_methodsExist) {
                setMethods();
            }

            this._useDefaultAccess = !_methodsExist;
            return _methodsExist;
        } else {
            this._useDefaultAccess = true;
            return true;
        }
    }

    public boolean changeUnsafeAccess(boolean enable) {
        this._canUnsafeAccess = enable;
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

    public boolean useDefaultAccess() {
        return _useDefaultAccess;
    }

    public boolean canUnsafeAccess() {
        return _canUnsafeAccess;
    }

    @Override
    public boolean canGetNewInstance() {
        if (_lazy) {
            changeGetNewInstance(true);
            this._lazy = false;
        }

        return _canGetNewInstance;
    }
}
