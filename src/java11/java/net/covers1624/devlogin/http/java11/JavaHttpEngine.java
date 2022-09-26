package net.covers1624.devlogin.http.java11;

import net.covers1624.devlogin.http.HttpEngine;
import net.covers1624.devlogin.http.HttpResponse;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

/**
 * Created by covers1624 on 15/9/22.
 */
public class JavaHttpEngine extends HttpEngine {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    protected HttpResponse makeRequest(String method, String url, byte @Nullable [] body, Map<String, String> headers) throws IOException {
        assert !HttpEngine.requiresRequestBody(method) || body != null : "HTTP Method" + method + " requires a body.";

        int i = 0;
        String[] interleavedHeaders = new String[headers.size() * 2];
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            interleavedHeaders[i++] = entry.getKey();
            interleavedHeaders[i++] = entry.getValue();
        }

        BodyPublisher bodyPublisher = body != null ? BodyPublishers.ofByteArray(body) : BodyPublishers.noBody();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .headers(interleavedHeaders)
                .method(method, bodyPublisher)
                .build();

        java.net.http.HttpResponse<byte[]> resp;
        try {
            resp = client.send(request, BodyHandlers.ofByteArray());
        } catch (InterruptedException ex) {
            throw new IOException("Request failed. Interrupted.", ex);
        }

        return new HttpResponse(
                resp.statusCode(),
                null,
                resp.body()
        );
    }

    @Override
    public void shutdown() throws IOException {
    }
}
