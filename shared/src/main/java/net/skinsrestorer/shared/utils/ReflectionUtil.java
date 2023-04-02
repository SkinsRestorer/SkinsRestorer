/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.shared.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtil {
    private static final Map<Class<?>, Class<?>> builtInMap = new HashMap<>();

    static {
        builtInMap.put(Integer.class, Integer.TYPE);
        builtInMap.put(Long.class, Long.TYPE);
        builtInMap.put(Double.class, Double.TYPE);
        builtInMap.put(Float.class, Float.TYPE);
        builtInMap.put(Boolean.class, Boolean.TYPE);
        builtInMap.put(Character.class, Character.TYPE);
        builtInMap.put(Byte.class, Byte.TYPE);
        builtInMap.put(Short.class, Short.TYPE);
    }

    private ReflectionUtil() {
    }

    public static boolean classExists(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Enum<?> getEnum(Class<?> clazz, String constant) throws ReflectiveOperationException {
        Enum<?>[] enumConstants = (Enum<?>[]) clazz.getEnumConstants();

        for (Enum<?> e : enumConstants) {
            if (e.name().equalsIgnoreCase(constant)) {
                return e;
            }
        }

        throw new ReflectiveOperationException(String.format("Enum constant not found %s", constant));
    }

    public static Enum<?> getEnum(Class<?> clazz, int ordinal) throws ReflectiveOperationException {
        try {
            return (Enum<?>) clazz.getEnumConstants()[ordinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ReflectiveOperationException(String.format("Enum constant not found %s", ordinal));
        }
    }

    public static Enum<?> getEnum(Class<?> clazz, String enumName, String constant) throws ReflectiveOperationException {
        return getEnum(getSubClass(clazz, enumName), constant);
    }

    private static Class<?> getSubClass(Class<?> clazz, String className) throws ReflectiveOperationException {
        for (Class<?> subClass : clazz.getDeclaredClasses()) {
            if (subClass.getSimpleName().equals(className))
                return subClass;
        }

        for (Class<?> subClass : clazz.getClasses()) {
            if (subClass.getSimpleName().equals(className))
                return subClass;
        }

        throw new ClassNotFoundException("Sub class " + className + " of " + clazz.getSimpleName() + " not found!");
    }

    public static Field getField(Class<?> clazz, String fieldName) throws ReflectiveOperationException {
        Field f;

        try {
            f = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            f = clazz.getField(fieldName);
        }

        f.setAccessible(true);

        return f;
    }

    private static Method getMethod(Class<?> clazz, String methodName) throws ReflectiveOperationException {
        Method m;
        try {
            m = clazz.getDeclaredMethod(methodName);
        } catch (Exception e) {
            m = clazz.getMethod(methodName);
        }

        m.setAccessible(true);
        return m;
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... args) throws ReflectiveOperationException {
        Method m;
        try {
            m = clazz.getDeclaredMethod(methodName, args);
        } catch (Exception e) {
            m = clazz.getMethod(methodName, args);
        }

        m.setAccessible(true);
        return m;
    }

    public static <T> T getObject(Object obj, String fieldName, Class<T> tClass) throws ReflectiveOperationException {
        return tClass.cast(getField(obj.getClass(), fieldName).get(obj));
    }

    public static Object getFieldByType(Object obj, String typeName) throws ReflectiveOperationException {
        return getFieldByType(obj, obj.getClass(), typeName);
    }

    private static Object getFieldByType(Object obj, Class<?> superClass, String typeName) throws ReflectiveOperationException {
        return getFieldByTypeList(obj, superClass, typeName).get(0);
    }

    public static List<Object> getFieldByTypeList(Object obj, String typeName) throws ReflectiveOperationException {
        return getFieldByTypeList(obj, obj.getClass(), typeName);
    }

    private static List<Object> getFieldByTypeList(Object obj, Class<?> superClass, String typeName) throws ReflectiveOperationException {
        List<Object> fields = new ArrayList<>();

        for (Field f : superClass.getDeclaredFields()) {
            if (f.getType().getSimpleName().equalsIgnoreCase(typeName)) {
                f.setAccessible(true);

                fields.add(f.get(obj));
            }
        }

        if (superClass.getSuperclass() != null) {
            fields.addAll(getFieldByTypeList(obj, superClass.getSuperclass(), typeName));
        }

        if (fields.isEmpty() && obj.getClass() == superClass) {
            throw new ReflectiveOperationException("Could not find field of type " + typeName + " in " + obj.getClass().getSimpleName());
        } else {
            return fields;
        }
    }

    public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initArgs) throws ReflectiveOperationException {
        return getConstructor(clazz, args).newInstance(initArgs);
    }

    public static Object invokeConstructor(Class<?> clazz, Object... initArgs) throws ReflectiveOperationException {
        return getConstructorByArgs(clazz, initArgs).newInstance(initArgs);
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args) throws ReflectiveOperationException {
        Constructor<?> c = clazz.getConstructor(args);
        c.setAccessible(true);

        return c;
    }

    private static Constructor<?> getConstructorByArgs(Class<?> clazz, Object... args) throws ReflectiveOperationException {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterTypes().length != args.length) {
                continue;
            }

            int i = 0;
            for (Class<?> parameter : constructor.getParameterTypes()) {
                if (!isAssignable(parameter, args[i])) {
                    break;
                }

                i++;
            }

            if (i == args.length) {
                return constructor;
            }
        }

        String argsString = Arrays.stream(args)
                .map(s -> s == null ? "null" : s.getClass().getSimpleName())
                .collect(Collectors.joining(", "));

        throw new ReflectiveOperationException(String.format("Could not find constructor with args %s in %s", argsString, clazz.getSimpleName()));
    }

    private static boolean isAssignable(Class<?> clazz, Object obj) {
        clazz = convertToPrimitive(clazz);

        return clazz.isInstance(obj) || clazz == convertToPrimitive(obj.getClass());
    }

    private static Class<?> convertToPrimitive(Class<?> clazz) {
        return builtInMap.getOrDefault(clazz, clazz);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method) throws ReflectiveOperationException {
        return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj);
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initArgs) throws ReflectiveOperationException {
        return Objects.requireNonNull(getMethod(clazz, method, args)).invoke(obj, initArgs);
    }

    public static Object invokeMethod(Object obj, String method) throws ReflectiveOperationException {
        return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj);
    }
}
