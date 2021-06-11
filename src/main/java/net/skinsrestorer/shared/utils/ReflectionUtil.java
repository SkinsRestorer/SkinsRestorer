/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.utils;

import li.cock.ie.reflect.DuckBypass;
import net.skinsrestorer.shared.exception.ReflectionException;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class ReflectionUtil {
    private static final DuckBypass reflect = new DuckBypass();
    public static String serverVersion = null;

    static {
        try {
            Class.forName("org.bukkit.Bukkit");
            serverVersion = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
        } catch (Exception ignored) {
        }
    }

    private ReflectionUtil() {
    }

    public static Class<?> getBukkitClass(String clazz) throws Exception {
        return Class.forName("org.bukkit.craftbukkit." + serverVersion + "." + clazz);
    }

    public static Class<?> getBungeeClass(String path, String clazz) throws Exception {
        return Class.forName("net.md_5.bungee." + path + "." + clazz);
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args) throws Exception {
        Constructor<?> c = clazz.getConstructor(args);
        c.setAccessible(true);

        return c;
    }

    public static Enum<?> getEnum(Class<?> clazz, String constant) throws Exception {
        Class<?> c = Class.forName(clazz.getName());
        Enum<?>[] econstants = (Enum<?>[]) c.getEnumConstants();
        for (Enum<?> e : econstants)
            if (e.name().equalsIgnoreCase(constant))
                return e;
        throw new Exception("Enum constant not found " + constant);
    }

    public static Enum<?> getEnum(Class<?> clazz, String enumname, String constant) throws Exception {
        Class<?> c = Class.forName(clazz.getName() + "$" + enumname);
        Enum<?>[] econstants = (Enum<?>[]) c.getEnumConstants();
        for (Enum<?> e : econstants)
            if (e.name().equalsIgnoreCase(constant))
                return e;
        throw new Exception("Enum constant not found " + constant);
    }

    public static Field getField(Class<?> clazz, String fname) throws Exception {
        Field f;

        try {
            f = clazz.getDeclaredField(fname);
        } catch (Exception e) {
            f = clazz.getField(fname);
        }

        setFieldAccessible(f);

        return f;
    }

    private static Method getMethod(Class<?> clazz, String mname) {
        Method m;
        try {
            m = clazz.getDeclaredMethod(mname);
        } catch (Exception e) {
            try {
                m = clazz.getMethod(mname);
            } catch (Exception ex) {
                return null;
            }
        }

        m.setAccessible(true);
        return m;
    }

    public static <T> Field getField(Class<?> target, String name, Class<T> fieldType, int index) {
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType()) && index-- <= 0) {
                field.setAccessible(true);
                return field;
            }
        }

        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(), name, fieldType, index);
        throw new IllegalArgumentException("Cannot find field with type " + fieldType);
    }

    private static Method getMethod(Class<?> clazz, String mname, Class<?>... args) {
        Method m;
        try {
            m = clazz.getDeclaredMethod(mname, args);
        } catch (Exception e) {
            try {
                m = clazz.getMethod(mname, args);
            } catch (Exception ex) {
                return null;
            }
        }

        m.setAccessible(true);
        return m;
    }

    public static Class<?> getNMSClass(String clazz) throws ReflectionException {
        try {
            return Class.forName("net.minecraft.server." + serverVersion + "." + clazz);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object getObject(Object obj, String fname) throws ReflectionException {
        try {
            return getField(obj.getClass(), fname).get(obj);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initargs) throws ReflectionException {
        try {
            return getConstructor(clazz, args).newInstance(initargs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initargs) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(clazz, method, args)).invoke(obj, initargs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Class<?> clazz, Object obj, String method, Object... initargs) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(clazz, method)).invoke(obj, initargs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Object obj, String method) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    public static Object invokeMethod(Object obj, String method, Object[] initargs) throws ReflectionException {
        try {
            return Objects.requireNonNull(getMethod(obj.getClass(), method)).invoke(obj, initargs);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private static void setFieldAccessible(Field f) {
        reflect.setEditable(f);
    }

    public static void setObject(Class<?> clazz, Object obj, String fname, Object value) {
        // getField(clazz, fname).set(obj, value);
        reflect.setValue(clazz, fname, obj, value);
    }
}
