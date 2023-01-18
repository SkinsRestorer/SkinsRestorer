package net.skinsrestorer.shared.connections.http;

import lombok.RequiredArgsConstructor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@RequiredArgsConstructor
public class HttpClient {
    private final String url;
    private final String requestBody;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final int timeout;

    public HttpResponse execute() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method.name());
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setDoInput(true);
        connection.setUseCaches(false);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }

        connection.setDoOutput(requestBody != null);
        if (requestBody != null) {
            try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
                output.writeBytes(requestBody);
            }
        }
        connection.connect();

        return new HttpResponse(connection.getResponseCode(), connection.getResponseMessage());
    }

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}
