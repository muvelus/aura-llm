package com.aura.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaService {
    // 2. Inject values from application.properties
    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String modelName;


    private final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callOllama(String promptText) {
        try {
            // 1. Prepare JSON data safely using a Map
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("model", "mistral");
            jsonMap.put("prompt", promptText);
            jsonMap.put("stream", false);

            String jsonInput = objectMapper.writeValueAsString(jsonMap);

            // 2. Create HTTP Client (Same as your code)
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                    .build();

            // 3. Send Request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Parse the "response" field from the JSON output
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                return rootNode.path("response").asText();
            } else {
                return "Error from Ollama: " + response.statusCode();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Internal Server Error: " + e.getMessage();
        }
    }
}