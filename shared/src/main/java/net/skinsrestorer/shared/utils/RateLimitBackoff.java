/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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

import net.skinsrestorer.shared.connections.http.HttpResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks per-endpoint rate limit backoff state.
 * When a 429 is received, the endpoint is marked as rate-limited
 * and subsequent requests are rejected until the backoff expires.
 */
public class RateLimitBackoff {
    private static final long DEFAULT_BACKOFF_MS = TimeUnit.SECONDS.toMillis(60);

    private final Map<String, Long> backoffUntil = new ConcurrentHashMap<>();

    /**
     * Check if the given endpoint is currently rate-limited.
     *
     * @param endpoint the endpoint identifier (e.g. URL string)
     * @return true if the endpoint is in a backoff period
     */
    public boolean isRateLimited(String endpoint) {
        Long until = backoffUntil.get(endpoint);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            backoffUntil.remove(endpoint, until);
            return false;
        }
        return true;
    }

    /**
     * Returns the remaining backoff time in milliseconds, or 0 if not rate-limited.
     */
    public long getRemainingMs(String endpoint) {
        Long until = backoffUntil.get(endpoint);
        if (until == null) {
            return 0;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            backoffUntil.remove(endpoint, until);
            return 0;
        }
        return remaining;
    }

    /**
     * Mark an endpoint as rate-limited based on an HTTP response.
     * Parses the Retry-After header if present, otherwise uses the default backoff.
     * Concurrent responses can extend an existing backoff but never shorten it.
     *
     * @param endpoint the endpoint identifier
     * @param response the HTTP response that triggered the rate limit
     */
    public void markRateLimited(String endpoint, HttpResponse response) {
        long backoffMs = parseRetryAfter(response.headers());
        long now = System.currentTimeMillis();
        long until = now + Math.min(backoffMs, Long.MAX_VALUE - now);
        backoffUntil.merge(endpoint, until, Math::max);
    }

    /**
     * Mark an endpoint as rate-limited with the default backoff duration.
     *
     * @param endpoint the endpoint identifier
     */
    public void markRateLimited(String endpoint) {
        backoffUntil.merge(endpoint, System.currentTimeMillis() + DEFAULT_BACKOFF_MS, Math::max);
    }

    /** Parses delay-seconds or an HTTP-date, falling back to a conservative delay for invalid headers. */
    private long parseRetryAfter(Map<String, List<String>> headers) {
        if (headers == null) {
            return DEFAULT_BACKOFF_MS;
        }

        List<String> retryAfterValues = null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Retry-After".equalsIgnoreCase(entry.getKey())) {
                retryAfterValues = entry.getValue();
                break;
            }
        }

        if (retryAfterValues == null || retryAfterValues.isEmpty()) {
            return DEFAULT_BACKOFF_MS;
        }

        String value = retryAfterValues.getFirst();
        if (value == null) {
            return DEFAULT_BACKOFF_MS;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            if (seconds >= 0) {
                return TimeUnit.SECONDS.toMillis(seconds);
            }
        } catch (NumberFormatException ignored) {
            try {
                long until = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli();
                return Math.max(0, until - System.currentTimeMillis());
            } catch (DateTimeParseException ignoredDate) {
                // Invalid Retry-After value; use the conservative default below.
            }
        }

        return DEFAULT_BACKOFF_MS;
    }
}
