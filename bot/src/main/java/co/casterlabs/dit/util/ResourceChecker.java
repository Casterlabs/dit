package co.casterlabs.dit.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class ResourceChecker {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static boolean checkMedia(String url) {
        try {
            HttpResponse<Void> response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build(),
                BodyHandlers.discarding()
            );

            String contentType = response.headers().firstValue("Content-Type").orElse("");

            boolean isOk = response.statusCode() >= 200 && response.statusCode() < 300;
            boolean isMedia = contentType.startsWith("image/") || contentType.startsWith("video/");

            return isOk && isMedia;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
