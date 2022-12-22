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
package net.skinsrestorer.api.exception;

/**
 * Thrown when a request for a skin has failed for some reason.
 * This does NOT mean that the player is not premium.
 * (This will never be thrown for that reason)
 * If the player is not premium, {@link NotPremiumException} is thrown.
 */
public class SkinRequestException extends Exception {
    private static final long serialVersionUID = -246848643846947966L;

    protected SkinRequestException() {
    }

    protected SkinRequestException(String message) {
        super(message);
    }

    protected SkinRequestException(Throwable cause) {
        super(cause);
    }
}
