package com.aura.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private OllamaService ollamaService;

    @PostMapping("/chat")
    public String generateResponse(@RequestBody ChatRequest request) {
        // 1. Receive the prompt from the API caller
        String userPrompt = request.getPrompt();

        // 2. Send it to the LLM via the Service
        String llmResponse = ollamaService.callOllama(userPrompt);

        // 3. Return the result
        return llmResponse;
    }
}