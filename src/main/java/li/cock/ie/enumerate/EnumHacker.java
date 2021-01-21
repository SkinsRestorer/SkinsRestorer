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

package li.cock.ie.enumerate;

import li.cock.ie.reflect.DuckBypass;

import java.lang.reflect.Field;

public class EnumHacker {
    private DuckBypass _bypass;
    private Field _ordinal;

    private boolean _lazy = true;

    public EnumHacker(DuckBypass bypass) {
        this._bypass = bypass;
    }

    public EnumHacker() {
        this(new DuckBypass());
    }

    public <E extends Enum<E>> E newInstance(Class<E> enumType, int ordinal, String name, Class<?>[] extraTypes, Object[] extraValues) {
        Class<?>[] types;
        Object[] values;

        try {
            types = new Class<?>[extraTypes.length + 2];
            types[0] = String.class;
            types[1] = int.class;
            System.arraycopy(extraTypes, 0, types, 2, extraTypes.length);

            values = new Object[extraValues.length + 2];
            values[0] = name;
            values[1] = ordinal;
            System.arraycopy(extraValues, 0, values, 2, extraValues.length);
        } catch (Throwable ex) {
            _bypass.process(ex);
            return null;
        }

        return _bypass.newInstance(enumType, types, values);
    }

    public <E extends Enum<E>> E newInstance(Class<E> enumType, int ordinal, String name, Object... extraValues) {
        return newInstance(enumType, ordinal, name, _bypass.getTypes(extraValues), extraValues);
    }

    private void cleanEnumCache(Class<? extends Enum<?>> enumType) {
        _bypass.setValue(enumType, "enumConstantDirectory");
        _bypass.setValue(enumType, "enumConstants");
    }

    protected boolean setValues(Class<? extends Enum<?>> enumType, Field valuesField, Object[] values) {
        if (_bypass.setValue(valuesField, values)) {
            cleanEnumCache(enumType);
            return true;
        }

        return false;
    }

    public Field getValuesField(Class<? extends Enum<?>> enumType) {
        Field valuesField = _bypass.getField(enumType, "$VALUES");
        if (valuesField == null) {
            valuesField = _bypass.getField(enumType, "ENUM$VALUES");
        }

        return valuesField;
    }

    @SuppressWarnings("unchecked")
    protected <E extends Enum<E>> E[] getValues(Class<E> enumType, Field valuesField) {
        return (E[]) _bypass.getValue(valuesField, (Object) enumType);
    }

    public <E extends Enum<E>> E[] getValues(Class<E> enumType) {
        return getValues(enumType, getValuesField(enumType));
    }

    public <E extends Enum<E>> boolean setValues(Class<E> enumType, E[] values) {
        return setValues(enumType, getValuesField(enumType), values);
    }

    public boolean setOrdinal(Enum<? extends Enum<?>> enumObj, int x) {
        if (enumObj == null) return false;

        if (_lazy) {
            _bypass.replaceMethod();
            this._ordinal = _bypass.getField(Enum.class, "ordinal");
            this._lazy = false;

            if (_ordinal == null) {
                return false;
            }
        } else if (_ordinal == null) {
            return false;
        }

        _bypass.setValue(_ordinal, enumObj, x);

        return false;
    }
}
