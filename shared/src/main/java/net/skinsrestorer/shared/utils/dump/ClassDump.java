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
package net.skinsrestorer.shared.utils.dump;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper class to get all information of a class.
 * Can be useful when needing to analyze NMS without being able to use a decompiler.
 */
public class ClassDump {
    /**
     * Generates an overview/dump of a class, displaying useful info about it.
     *
     * @param clazz    The class to be inspected.
     * @param instance Optional instance of the class to be dumped. Allows displaying of field values.
     * @param <C>      The classes generic type.
     * @return String with newlines containing the classes dump.
     */
    public static <C> String dumpCLass(Class<C> clazz, @Nullable C instance) {
        return ClassDump.dumpCLass(clazz, instance, "");
    }

    private static <C> String dumpCLass(Class<C> clazz, @Nullable C instance, String prefix) {
        String deeperPrefix = prefix + "  ";
        StringBuilder result = new StringBuilder(prefix.equals("") ? "\n" : "");

        result.append(prefix).append("Classname: ").append(clazz.getSimpleName()).append("\n");

        Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {
            result.append(prefix).append("Fields: ").append("\n");
            for (Field field : fields) {
                result.append(deeperPrefix).append(field.getName()).append(" | ").append(field.getType().getSimpleName());

                if (instance != null) {
                    try {
                        result.append(": ");
                        field.setAccessible(true);
                        result.append(field.get(instance));
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }

                result.append("\n");
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            result.append(prefix).append("Methods: ").append("\n");
            for (Method method : methods) {
                result.append(deeperPrefix).append(method.getName()).append(" | ").append(method.getReturnType().getSimpleName()).append("\n");
            }
        }

        C[] constants = clazz.getEnumConstants();
        if (constants != null) {
            result.append(prefix).append("Enum Constants: ").append("\n");
            for (C constant : constants) {
                result.append(deeperPrefix).append(constant.getClass().getSimpleName()).append("\n");
            }
        }

        Class<?>[] children = clazz.getDeclaredClasses();
        if (children.length > 0) {
            result.append("Child Classes: ").append("\n");
            for (Class<?> clazz2 : children) {
                result.append(deeperPrefix).append(clazz2.getSimpleName()).append("\n");
                result.append(dumpCLass(clazz2, null, deeperPrefix + "  "));
            }
        }

        return result.toString();
    }
}
