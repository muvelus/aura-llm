package com.aura.llm;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Picks which {@link LlmService} handles each request.
 *
 * <p>The active provider is seeded from the {@code llm.provider} property at
 * startup, so switching backends is a one-line change in application.properties.
 * It can also be changed at runtime via {@link #setProvider(String)} (exposed by
 * the controller) without redeploying the application.</p>
 */
@Service
public class LlmRouter {

    private static final String OLLAMA = "ollama";
    private static final String BEDROCK = "bedrock";

    private final OllamaService ollamaService;
    private final BedrockService bedrockService;

    @Value("${llm.provider:ollama-mistral}")
    private String configuredProvider;

    private volatile String activeProvider;

    public LlmRouter(OllamaService ollamaService, BedrockService bedrockService) {
        this.ollamaService = ollamaService;
        this.bedrockService = bedrockService;
    }

    @PostConstruct
    void init() {
        this.activeProvider = normalize(configuredProvider);
    }

    /** Route a prompt to whichever provider is currently active. */
    public String generate(String prompt) {
        return resolve(activeProvider).generate(prompt);
    }

    public String getActiveProvider() {
        return activeProvider;
    }

    /** Switch the active provider at runtime. Returns the normalized value applied. */
    public String setProvider(String provider) {
        this.activeProvider = normalize(provider);
        return this.activeProvider;
    }

    private LlmService resolve(String provider) {
        return BEDROCK.equals(provider) ? bedrockService : ollamaService;
    }

    /** Anything starting with "bedrock" selects Bedrock; everything else stays on Ollama. */
    private String normalize(String provider) {
        if (provider != null && provider.trim().toLowerCase().startsWith(BEDROCK)) {
            return BEDROCK;
        }
        return OLLAMA;
    }
}
