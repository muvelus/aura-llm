package com.aura.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class Sample {

    public static void main(String[] args) {
        // 1. Configuration
        String modelName = "mistral";
        String promptText = "Why is the sky blue?"; // Your message here
        String ollamaUrl = "http://127.0.0.1:11434/api/generate";

        // 2. Build the JSON Payload
        // We use "stream": false to get the full response at once rather than a stream of tokens.
        // In a real app, use a library like Jackson or Gson for JSON processing.
        String jsonPayload = String.format(
                "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                modelName, promptText
        );

        // 3. Create the HTTP Client
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 4. Build the POST Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(jsonPayload))
                .build();

        // 5. Send Request and Handle Response
        try {
            System.out.println("Sending message to Ollama...");
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                System.out.println("--- Raw Response ---");
                System.out.println(responseBody);

                // Simple extraction of the "response" field (quick-and-dirty without external libraries)
                String actualResponse = extractResponseText(responseBody);
                System.out.println("\n--- Extracted Message ---");
                System.out.println(actualResponse);
            } else {
                System.err.println("Error: " + response.statusCode());
                System.err.println(response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to extract the "response" value from JSON string manually
    private static String extractResponseText(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key);
        if (start == -1) return "Response field not found";

        start += key.length();
        int end = json.indexOf("\",", start); // Finds the ending quote and comma

        if (end == -1) end = json.lastIndexOf("\""); // Fallback for end of JSON

        String extracted = json.substring(start, end);

        // Unescape common JSON characters for readability
        return extracted.replace("\\n", "\n").replace("\\\"", "\"");
    }
}