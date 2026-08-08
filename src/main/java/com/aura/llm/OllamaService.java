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
public class OllamaService implements LlmService {
    // 2. Inject values from application.properties
    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String modelName;

    // Adapts the existing Ollama call to the common LlmService contract.
    @Override
    public String generate(String prompt) {
        return callOllama(prompt);
    }


    private final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. Define the client ONCE as a constant or field
//    private static final HttpClient CLIENT = HttpClient.newBuilder()
//            .version(HttpClient.Version.HTTP_2)
//            .build();

    public String callOllama(String promptText) {
        try {
            HttpClient CLIENT = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("model", modelName);
            jsonMap.put("prompt", promptText);
            jsonMap.put("stream", false);

            String jsonInput = objectMapper.writeValueAsString(jsonMap);

            // 2. Reuse the existing CLIENT
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                    .build();

            // The send() method will now use connection pooling automatically
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                return rootNode.path("response").asText();
            }
            return "Error from Ollama: " + response.statusCode();

        } catch (Exception e) {
            e.printStackTrace();
            return "Internal Server Error: " + e.getMessage();
        }
    }
}