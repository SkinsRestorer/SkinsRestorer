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

import java.lang.reflect.*;

public interface IAccess {
    Object getValue(Field target, Object obj);
    boolean setValue(Field target, Object obj, Object value);

    boolean setModifiers(Field target, int mod);
    boolean setModifiers(Method target, int mod);

    Object getNewInstance(Constructor<?> target, Object... args);

    boolean changeGetValue(boolean enable);
    boolean changeSetValue(boolean enable);
    boolean changeSetFieldModifiers(boolean enable);
    boolean changeSetMethodModifiers(boolean enable);
    boolean changeGetNewInstance(boolean enable);

    boolean canGetValue();
    boolean canSetValue();
    boolean canSetFieldModifiers();
    boolean canSetMethodModifiers();
    boolean canGetNewInstance();
}