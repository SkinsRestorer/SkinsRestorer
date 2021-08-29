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

import li.cock.ie.reflect.DuckReflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DefaultImpl implements IAccess {
    protected DuckReflect _reflect;

    private boolean _canGetValue = true;
    private boolean _canSetValue = true;
    private boolean _canSetFieldModifiers = false;
    private boolean _canSetMethodModifiers = false;
    private boolean _canGetNewInstance = true;

    private boolean _lazy;

    private Field _fieldModifiers = null;
    private Field _methodModifiers = null;

    public DefaultImpl(DuckReflect reflect, boolean canSetModifiers, boolean lazy) {
        this._reflect = reflect;
        this._lazy = (canSetModifiers & lazy);

        if (canSetModifiers & !_lazy) {
            changeSetFieldModifiers(true);
            changeSetMethodModifiers(true);
        }
    }

    public DefaultImpl(DuckReflect reflect, boolean canSetModifiers) {
        this(reflect, canSetModifiers, true);
    }

    @Override
    public Object getValue(Field target, Object obj) {
        if (!_canGetValue) return null;
        return _reflect.getValue(target, obj);
    }

    @Override
    public boolean setValue(Field target, Object obj, Object value) {
        if (!_canSetValue) return false;
        return _reflect.setValue(target, obj, value);
    }

    @Override
    public boolean setModifiers(Field target, int mod) {
        if (!_canSetFieldModifiers) {
            if (_lazy) {
                changeSetFieldModifiers(true);
                changeSetMethodModifiers(true);
                this._lazy = false;

                _reflect.reset();
            } else {
                return false;
            }
        }

        return _reflect.setValue(_fieldModifiers, target, mod);
    }

    @Override
    public boolean setModifiers(Method target, int mod) {
        if (!_canSetMethodModifiers) {
            if (_lazy) {
                changeSetFieldModifiers(true);
                changeSetMethodModifiers(true);
                this._lazy = false;

                _reflect.reset();
            } else {
                return false;
            }
        }

        return _reflect.setValue(_methodModifiers, target, mod);
    }

    @Override
    public Object getNewInstance(Constructor<?> target, Object... args) {
        if (!_canGetNewInstance) return null;
        return _reflect.newInstance(target, args);
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
        if (enable) {
            if (_fieldModifiers == null) {
                this._fieldModifiers = _reflect.getField(Field.class, "modifiers");
            }

            this._canSetFieldModifiers = (_fieldModifiers != null);
            return _canSetFieldModifiers;
        } else {
            this._canSetFieldModifiers = false;
            return true;
        }
    }

    @Override
    public boolean changeSetMethodModifiers(boolean enable) {
        if (enable) {
            if (_methodModifiers == null) {
                this._methodModifiers = _reflect.getField(Method.class, "modifiers");
            }

            this._canSetMethodModifiers = (_methodModifiers != null);
            return _canSetMethodModifiers;
        } else {
            this._canSetMethodModifiers = false;
            return true;
        }
    }

    @Override
    public boolean changeGetNewInstance(boolean enable) {
        this._canGetNewInstance = enable;
        return true;
    }

}
