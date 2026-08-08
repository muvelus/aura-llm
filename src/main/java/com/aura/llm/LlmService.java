package com.aura.llm;

/**
 * Common contract for every LLM backend (Ollama, Amazon Bedrock, ...).
 * Implementations are interchangeable so the active provider can be swapped
 * via configuration or at runtime without touching the calling code.
 */
public interface LlmService {

    /**
     * Send a prompt to the underlying model and return its reply.
     *
     * @param prompt the user's prompt text
     * @return the model's response (free text for Ollama, structured JSON for Bedrock)
     */
    String generate(String prompt);
}
