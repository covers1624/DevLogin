package net.covers1624.devlogin.http;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

/**
 * Created by covers1624 on 12/9/22.
 */
public abstract class HttpEngine {

    private static final List<String> BUILTIN_ENGINES = asList(
            "net.covers1624.devlogin.http.java11.JavaHttpEngine",
            "net.covers1624.devlogin.http.apache.ApacheHttpEngine"
    );
    private static final String MANUAL_ENGINE = System.getProperty("devlogin.http_engine");

    public static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_JSON = "application/json";

    public final HttpResponse getJson(String url, Map<String, String> headers) throws IOException {
        headers = new HashMap<>(headers);
        headers.put("Content-Type", APPLICATION_JSON);
        headers.put("Accept", APPLICATION_JSON);

        return makeRequest("GET", url, null, headers);
    }

    public final HttpResponse postJson(String url, Gson gson, Map<String, Object> body) throws IOException {
        return postJson(url, gson, body, new HashMap<>());
    }

    public final HttpResponse postJson(String url, Gson gson, Map<String, Object> body, Map<String, String> headers) throws IOException {
        headers = new HashMap<>(headers);
        headers.put("Content-Type", APPLICATION_JSON);
        headers.put("Accept", APPLICATION_JSON);

        return makeRequest("POST", url, gson.toJson(body).getBytes(UTF_8), headers);
    }

    public final HttpResponse postForm(String url, Map<String, String> body) throws IOException {
        return postForm(url, body, new HashMap<>());
    }

    public final HttpResponse postForm(String url, Map<String, String> body, Map<String, String> headers) throws IOException {
        headers = new HashMap<>(headers);
        headers.put("Content-Type", FORM_URL_ENCODED);

        return makeRequest("POST", url, formEncode(body).getBytes(UTF_8), headers);
    }

    protected abstract HttpResponse makeRequest(String method, String url, byte @Nullable [] body, Map<String, String> headers) throws IOException;

    public abstract void shutdown() throws IOException;

    public static String formEncode(Map<String, String> query) {
        try {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (builder.length() > 0) {
                    builder.append("&");
                }
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                builder.append("=");
                builder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            // This is impossible, UTF-8 is guaranteed to be supported.
            throw new RuntimeException("Wot?", e);
        }
    }

    public static boolean requiresRequestBody(String method) {
        return method.equalsIgnoreCase("POST")
                || method.equalsIgnoreCase("PUT")
                || method.equalsIgnoreCase("PATCH")
                || method.equalsIgnoreCase("PROPPATCH")
                || method.equalsIgnoreCase("REPORT");
    }

    public static HttpEngine selectEngine() {
        HttpEngine engine;
        if (MANUAL_ENGINE != null) {
            System.out.println("[DevLogin] Sysprop overriding HttpEngine to: " + MANUAL_ENGINE);
            engine = tryLoad(MANUAL_ENGINE);
            if (engine == null) {
                System.exit(1);
            }
            return engine;
        }

        for (String builtinEngine : BUILTIN_ENGINES) {
            engine = tryLoad(builtinEngine);
            if (engine != null) {
                return engine;
            }
        }
        System.out.println("No HttpEngine could be loaded. Unable to continue.");
        System.exit(1);

        return null;
    }

    private static HttpEngine tryLoad(String cName) {
        System.out.println("[DevLogin] Trying to load HttpEngine: " + cName);
        try {
            Class<?> clazz = Class.forName(cName);
            return (HttpEngine) clazz.getConstructor().newInstance();
        } catch (Throwable ex) {
            System.out.println("[DevLogin] failed to load HttpEngine: " + cName);
            return null;
        }
    }
}
