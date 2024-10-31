package io.greitan.mineserv.network;

import io.greitan.mineserv.utils.Logger;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Network {
    private static final Gson gson = new Gson();

    public static boolean sendPostRequest(String url, Object data) {
        try {
            String jsonData = gson.toJson(data);

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Logger.debug("Request: " + jsonData);
            Logger.debug("Status Code: " + response.statusCode());

            int statusCode = response.statusCode();
            return statusCode == 200 || statusCode == 202;
            
        } catch (Exception e) {
            Logger.error("Cannot connect to voice chat server!");
            return false;
        }
    }
}
