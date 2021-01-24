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

package li.cock.ie.reflect;

public class DuckHandler implements IDuckHandler {
    private Throwable ex = null;
    private boolean debug;

    public DuckHandler(boolean debug) {
        this.debug = debug;
    }

    public DuckHandler() {
        this(false);
    }

    @Override
    public void process(Throwable ex) {
        if (debug) {
            ex.printStackTrace(System.err);
        }

        this.ex = ex;
    }

    @Override
    public void reset() {
        this.ex = null;
    }

    @Override
    public boolean check() {
        boolean isSuccess = (ex == null);
        reset();

        return isSuccess;
    }
}
