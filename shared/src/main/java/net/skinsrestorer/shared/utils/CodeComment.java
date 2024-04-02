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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to provide comments about code elements such as classes, methods, constructors,
 * and fields.
 *
 * <p>
 * The comments should be specified as an array of strings in the {@link #value()} field of this annotation.
 * </p>
 *
 * <p>
 * This annotation is intended to be used at runtime and can be applied to various code elements using the
 * {@link ElementType} annotations parameter. It can be used to store important information or additional
 * explanations about the code for future reference or debugging purposes.
 * </p>
 *
 * <p>
 * This annotation is retained at runtime, which means others can see it in compiled class files.
 * </p>
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface CodeComment {
    @SuppressWarnings("unused")
    String[] value();
}
