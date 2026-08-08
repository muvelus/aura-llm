package com.aura.llm;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final LlmRouter llmRouter;

    public ChatController(LlmRouter llmRouter) {
        this.llmRouter = llmRouter;
    }

    @PostMapping("/chat")
    public String generateResponse(@RequestBody ChatRequest request) {
        // 1. Receive the prompt from the API caller
        String userPrompt = request.getPrompt();

        // 2. Send it to whichever LLM is currently active (Ollama or Bedrock)
        String llmResponse = llmRouter.generate(userPrompt);

        // 3. Return the result
        return llmResponse;
    }

    /** Show which LLM provider is currently active. */
    @GetMapping("/provider")
    public String currentProvider() {
        return llmRouter.getActiveProvider();
    }

    /**
     * Switch the active LLM provider at runtime (no redeploy needed).
     * Example: POST /api/provider?name=bedrock  or  POST /api/provider?name=ollama
     */
    @PostMapping("/provider")
    public String switchProvider(@RequestParam String name) {
        return "Active LLM provider is now: " + llmRouter.setProvider(name);
    }
}
