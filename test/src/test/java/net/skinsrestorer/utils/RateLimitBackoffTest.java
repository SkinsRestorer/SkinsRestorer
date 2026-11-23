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
package net.skinsrestorer.utils;

import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.utils.RateLimitBackoff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitBackoffTest {
    private final RateLimitBackoff backoff = new RateLimitBackoff();

    @ParameterizedTest
    @ValueSource(strings = {"Retry-After", "retry-after", "ReTrY-aFtEr"})
    void retryAfterHeadersSetTheEndpointBackoff(String header) {
        backoff.markRateLimited("lookup", response(header, "60"));

        assertTrue(backoff.isRateLimited("lookup"));
        assertTrue(backoff.getRemainingMs("lookup") > 50_000);
        assertEquals(0, backoff.getRemainingMs("other"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "-1", "999999999999999999999"})
    void invalidRetryAfterUsesTheDefaultBackoff(String value) {
        backoff.markRateLimited("lookup", response("Retry-After", value));

        assertTrue(backoff.getRemainingMs("lookup") > 50_000);
    }

    @Test
    void laterShorterResponsesCannotReduceAnExistingBackoff() {
        backoff.markRateLimited("lookup", response("Retry-After", "600"));
        backoff.markRateLimited("lookup", response("Retry-After", "1"));
        backoff.markRateLimited("lookup");

        assertTrue(backoff.getRemainingMs("lookup") > 500_000);
    }

    @Test
    void hugeRetryAfterCannotOverflowIntoAnExpiredBackoff() {
        backoff.markRateLimited("lookup", response("Retry-After", Long.toString(Long.MAX_VALUE)));

        assertTrue(backoff.isRateLimited("lookup"));
        assertTrue(backoff.getRemainingMs("lookup") > 60_000);
    }

    @Test
    void zeroRetryAfterAllowsAnImmediateRetry() {
        backoff.markRateLimited("lookup", response("Retry-After", "0"));

        assertEquals(0, backoff.getRemainingMs("lookup"));
        assertFalse(backoff.isRateLimited("lookup"));
    }

    @Test
    void httpDateRetryAfterUsesTheServerDeadline() {
        String date = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(120)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        backoff.markRateLimited("lookup", response("Retry-After", date));

        assertTrue(backoff.getRemainingMs("lookup") > 100_000);
    }

    @Test
    void expiredHttpDateAllowsAnImmediateRetry() {
        backoff.markRateLimited("lookup", response("Retry-After", "Wed, 21 Oct 2015 07:28:00 GMT"));

        assertEquals(0, backoff.getRemainingMs("lookup"));
    }

    private HttpResponse response(String header, String value) {
        return new HttpResponse(429, "{}", Map.of(header, List.of(value)));
    }
}
