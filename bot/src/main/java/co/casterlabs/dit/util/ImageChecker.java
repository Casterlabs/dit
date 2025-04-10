package co.casterlabs.dit.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class ImageChecker {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static boolean check(String url) {
        try {
            int statusCode = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build(),
                BodyHandlers.discarding()
            ).statusCode();

            return statusCode >= 200 && statusCode < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
