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

import java.lang.reflect.*;
import java.util.*;

public class EnumList<E extends Enum<E>> implements List<E> {
    private EnumHacker _hack;
    private Class<E> _enumType;

    private int _lazyLevel;

    private Field _valuesField;

    private List<E> _values = new ArrayList<E>();

    public EnumList(EnumHacker hack, Class<E> enumType, int lazyLevel) {
        this._hack = hack;
        this._enumType = enumType;

        this._lazyLevel = lazyLevel;

        this._valuesField = _hack.getValuesField(_enumType);

        pull();
    }

    public EnumList(EnumHacker hack, Class<E> enumType) {
        this(hack, enumType, 1);
    }

    public EnumList(Class<E> enumType, int lazyLevel) {
        this(new EnumHacker(), enumType, lazyLevel);
    }

    public EnumList(Class<E> enumType) {
        this(enumType, 1);
    }

    public void pull() {
        E[] values = _hack.getValues(_enumType, _valuesField);

        _values.clear();
        Collections.addAll(_values, values);
    }

    @SuppressWarnings("unchecked")
    public void push() {
        E[] values = (E[]) Array.newInstance(_enumType, _values.size());
        _values.toArray(values);

        for(int i = 0; i < values.length; ++i) {
            _hack.setOrdinal(values[i], i);
        }

        _hack.setValues(_enumType, _valuesField, values);
    }

    @Override
    public int size() {
        if(_lazyLevel > 1) pull();
        return _values.size();
    }

    @Override
    public boolean isEmpty() {
        if(_lazyLevel > 1) pull();
        return _values.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if(_lazyLevel > 1) pull();
        return _values.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        if(_lazyLevel > 1) pull();
        return _values.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if(_lazyLevel > 1) pull();
        return _values.toArray(a);
    }

    public E newInstance(int ordinal, String name, Class<?>[] argTypes, Object... extraValues) {
        return _hack.newInstance(_enumType, ordinal, name, argTypes, extraValues);
    }

    public E newInstance(int ordinal, String name, Object... extraValues) {
        return _hack.newInstance(_enumType, ordinal, name, extraValues);
    }

    public E add(String name, Class<?>[] argTypes, Object... extraValues) {
        if(_lazyLevel > 1) pull();

        E newEnum = newInstance(_values.size(), name, argTypes, extraValues);

        boolean success = _values.add(newEnum);
        if(_lazyLevel > 0) push();

        return newEnum;
    }

    public E add(String name, Object... extraValues) {
        if(_lazyLevel > 1) pull();

        E newEnum = newInstance(_values.size(), name, extraValues);

        _values.add(newEnum);
        if(_lazyLevel > 0) push();

        return newEnum;
    }

    @Override
    public boolean add(E e) {
        if(_lazyLevel > 1) pull();

        boolean success = _values.add(e);
        if(success & (_lazyLevel > 0)) push();

        return success;
    }

    @Override
    public boolean remove(Object o) {
        if(_lazyLevel > 1) pull();

        boolean success = _values.remove(o);
        if(success & (_lazyLevel > 0)) push();

        return success;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if(_lazyLevel > 1) pull();
        return _values.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if(_lazyLevel > 1) pull();

        boolean success = _values.addAll(c);
        if(success & (_lazyLevel > 0)) push();

        return success;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if(_lazyLevel > 1) pull();

        boolean success = _values.addAll(index, c);
        if(success & (_lazyLevel > 0)) push();

        return success;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if(_lazyLevel > 1) pull();

        boolean success = _values.removeAll(c);
        if(success & (_lazyLevel > 0)) push();

        return success;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if(_lazyLevel > 1) pull();

        boolean success = _values.retainAll(c);
        if(success & (_lazyLevel > 0)) push();

        return success;
    }

    @Override
    public void clear() {
        _values.clear();
        if(_lazyLevel > 0) push();
    }

    @Override
    public E get(int index) {
        if(_lazyLevel > 1) pull();
        return _values.get(index);
    }

    public E set(int index, String name, Class<?>[] argTypes, Object... extraValues) {
        if(_lazyLevel > 1) pull();

        E result = _values.set(index, newInstance(index, name, argTypes, extraValues));
        if(_lazyLevel > 0) push();

        return result;
    }

    public E set(int index, String name, Object... extraValues) {
        if(_lazyLevel > 1) pull();

        E result = _values.set(index, newInstance(index, name, extraValues));
        if(_lazyLevel > 0) push();

        return result;
    }

    @Override
    public E set(int index, E element) {
        if(_lazyLevel > 1) pull();

        E result = _values.set(index, element);
        if(_lazyLevel > 0) push();

        return result;
    }

    public E add(int index, String name, Class<?>[] argTypes, Object... extraValues) {
        if(_lazyLevel > 1) pull();

        E newEnum = newInstance(index, name, argTypes, extraValues);
        _values.add(index, newEnum);

        if(_lazyLevel > 0) push();

        return newEnum;
    }

    public E add(int index, String name, Object... extraValues) {
        if(_lazyLevel > 1) pull();

        E newEnum = newInstance(index, name, extraValues);
        _values.add(index, newEnum);

        if(_lazyLevel > 0) push();

        return newEnum;
    }

    @Override
    public void add(int index, E element) {
        if(_lazyLevel > 1) pull();
        _values.add(index, element);
        if(_lazyLevel > 0) push();
    }

    @Override
    public E remove(int index) {
        if(_lazyLevel > 1) pull();

        E result = _values.remove(index);
        if(_lazyLevel > 0) push();

        return result;
    }

    @Override
    public int indexOf(Object o) {
        if(_lazyLevel > 1) pull();
        return _values.indexOf(0);
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if(_lazyLevel > 1) pull();
        return _values.subList(fromIndex, toIndex);
    }
}
