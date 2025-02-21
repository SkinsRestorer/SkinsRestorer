/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtil {
    private static final Map<Class<?>, Class<?>> wrap2primitiveMap = new HashMap<>();

    static {
        wrap2primitiveMap.put(Integer.class, Integer.TYPE);
        wrap2primitiveMap.put(Long.class, Long.TYPE);
        wrap2primitiveMap.put(Double.class, Double.TYPE);
        wrap2primitiveMap.put(Float.class, Float.TYPE);
        wrap2primitiveMap.put(Boolean.class, Boolean.TYPE);
        wrap2primitiveMap.put(Character.class, Character.TYPE);
        wrap2primitiveMap.put(Byte.class, Byte.TYPE);
        wrap2primitiveMap.put(Short.class, Short.TYPE);
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

    public static boolean classExists(String... classNames) {
        for (String className : classNames) {
            if (classExists(className)) {
                return true;
            }
        }

        return false;
    }

    public static Enum<?> getEnum(Class<?> clazz, String constant) throws ReflectiveOperationException {
        Enum<?>[] enumConstants = (Enum<?>[]) clazz.getEnumConstants();

        for (Enum<?> e : enumConstants) {
            if (e.name().equalsIgnoreCase(constant)) {
                return e;
            }
        }

        throw new ReflectiveOperationException("Enum constant not found %s".formatted(constant));
    }

    public static Enum<?> getEnum(Class<?> clazz, int ordinal) throws ReflectiveOperationException {
        try {
            return (Enum<?>) clazz.getEnumConstants()[ordinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ReflectiveOperationException("Enum constant not found %s".formatted(ordinal));
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

        throw new ClassNotFoundException("Sub class %s of %s not found!".formatted(className, clazz.getSimpleName()));
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

    @SuppressWarnings("unchecked")
    public static <T> T getObject(Object obj, String fieldName) throws ReflectiveOperationException {
        return (T) getField(obj.getClass(), fieldName).get(obj);
    }

    public static Object getFieldByType(Object obj, String typeName) throws ReflectiveOperationException {
        return getFieldByType(obj, obj.getClass(), typeName);
    }

    private static Object getFieldByType(Object obj, Class<?> superClass, String typeName) throws ReflectiveOperationException {
        return getFieldByTypeList(obj, superClass, typeName).getFirst();
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
            throw new ReflectiveOperationException("Could not find field of type %s in %s".formatted(typeName, obj.getClass().getSimpleName()));
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

        throw new ReflectiveOperationException("Could not find constructor with args %s in %s".formatted(argsString, clazz.getSimpleName()));
    }

    private static boolean isAssignable(Class<?> clazz, Object obj) {
        clazz = convertToPrimitive(clazz);

        return clazz.isInstance(obj) || clazz == convertToPrimitive(obj.getClass());
    }

    private static Class<?> convertToPrimitive(Class<?> clazz) {
        return wrap2primitiveMap.getOrDefault(clazz, clazz);
    }

    public static Object invokeObjectMethod(@NotNull Object obj, String method, ParameterPair<?>... parameters) throws ReflectiveOperationException {
        return getMethod(obj.getClass(), method, ParameterPair.classesFromArgs(parameters)).invoke(obj, ParameterPair.valuesFromArgs(parameters));
    }

    public static Object invokeStaticMethod(@NotNull Class<?> clazz, String method, ParameterPair<?>... parameters) throws ReflectiveOperationException {
        return getMethod(clazz, method, ParameterPair.classesFromArgs(parameters)).invoke(null, ParameterPair.valuesFromArgs(parameters));
    }

    public record ParameterPair<P>(Class<?> clazz, P value) {
        public ParameterPair(P value) {
            this(value.getClass(), value);
        }

        private static Class<?>[] classesFromArgs(ParameterPair<?>... args) {
            return Arrays.stream(args)
                    .map(ParameterPair::clazz)
                    .toArray(Class<?>[]::new);
        }

        private static Object[] valuesFromArgs(ParameterPair<?>... args) {
            return Arrays.stream(args)
                    .map(ParameterPair::value)
                    .toArray(Object[]::new);
        }
    }
}
