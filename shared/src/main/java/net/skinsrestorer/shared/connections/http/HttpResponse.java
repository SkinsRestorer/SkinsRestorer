package net.skinsrestorer.shared.connections.http;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HttpResponse {
    private static final Gson GSON = new Gson();
    private final int statusCode;
    private final String body;
    private final

    public <T> T getBodyAs(Class<T> clazz) {
        return GSON.fromJson(body, clazz);
    }
}
