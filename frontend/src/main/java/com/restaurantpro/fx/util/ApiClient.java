package com.restaurantpro.fx.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

public class ApiClient {

    private static final String BASE_URL = "http://localhost:8081/api";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static String jwtToken = null;

    public static void setToken(String token) {
        jwtToken = token;
        System.out.println("[ApiClient] Token set: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
    }
    public static String getToken() { return jwtToken; }

    private static HttpRequest.Builder addAuth(HttpRequest.Builder b) {
        if (jwtToken != null && !jwtToken.isEmpty()) {
            b.header("Authorization", "Bearer " + jwtToken);
        }
        return b;
    }

    public static String get(String path) throws Exception {
        System.out.println("[ApiClient] GET " + path + " | token=" + (jwtToken != null ? "OK" : "NULL"));
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10)).GET();
        addAuth(b);
        HttpResponse<String> response = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            System.err.println("[ApiClient] ERROR " + response.statusCode() + " on GET " + path);
        }
        return response.body();
    }

    public static String post(String path, Object bodyObj) throws Exception {
        String json = bodyObj == null ? "{}" : MAPPER.writeValueAsString(bodyObj);
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addAuth(b);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    public static String put(String path, Object bodyObj) throws Exception {
        String json = bodyObj == null ? "{}" : MAPPER.writeValueAsString(bodyObj);
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        addAuth(b);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    public static String patch(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.noBody());
        addAuth(b);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    public static int delete(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10)).DELETE();
        addAuth(b);

        HttpResponse<String> response = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new Exception(response.body());
        }

        return response.statusCode();
    }

    public static String deleteWithBody(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10)).DELETE();
        addAuth(b);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    public static Map<String, Object> parseMap(String json) throws Exception {
        return MAPPER.readValue(json, Map.class);
    }

    public static <T> T parse(String json, Class<T> clazz) throws Exception {
        return MAPPER.readValue(json, clazz);
    }
}
