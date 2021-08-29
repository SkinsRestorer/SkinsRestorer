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
package li.cock.ie.reflect;

public class DuckHandler implements IDuckHandler {
    private Throwable ex = null;
    private final boolean debug;

    public DuckHandler(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void process(Throwable ex) {
        if (debug) {
            ex.printStackTrace();
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
