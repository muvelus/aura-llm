package com.aura.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LlmRouter} provider selection — pure unit tests, no
 * Spring context. Backends are lightweight test doubles (subclasses overriding
 * {@code generate}) that record the prompt they received, so we can assert which
 * backend the router routed to. The configured provider is injected via
 * {@link ReflectionTestUtils} (standing in for the {@code @Value} binding) and
 * {@link LlmRouter#init()} is invoked manually to mimic {@code @PostConstruct}.
 */
class LlmRouterTest {

    /** Records the prompt passed to it and returns a recognizable reply. */
    private static final class RecordingOllama extends OllamaService {
        String received;

        @Override
        public String generate(String prompt) {
            this.received = prompt;
            return "ollama-reply";
        }
    }

    private static final class RecordingBedrock extends BedrockService {
        String received;

        @Override
        public String generate(String prompt) {
            this.received = prompt;
            return "bedrock-reply";
        }
    }

    private RecordingOllama ollamaService;
    private RecordingBedrock bedrockService;
    private LlmRouter router;

    @BeforeEach
    void setUp() {
        ollamaService = new RecordingOllama();
        bedrockService = new RecordingBedrock();
        router = new LlmRouter(ollamaService, bedrockService);
    }

    /** Drives init() with the given configured value (mirrors @Value + @PostConstruct). */
    private void startWith(String configuredProvider) {
        ReflectionTestUtils.setField(router, "configuredProvider", configuredProvider);
        router.init();
    }

    @Test
    void defaultsToOllamaForExistingConfigValue() {
        startWith("ollama-mistral");

        assertThat(router.getActiveProvider()).isEqualTo("ollama");
        assertThat(router.generate("hi")).isEqualTo("ollama-reply");
        assertThat(ollamaService.received).isEqualTo("hi");
        assertThat(bedrockService.received).isNull();
    }

    @Test
    void routesToBedrockWhenConfigured() {
        startWith("bedrock");

        assertThat(router.getActiveProvider()).isEqualTo("bedrock");
        assertThat(router.generate("hi")).isEqualTo("bedrock-reply");
        assertThat(bedrockService.received).isEqualTo("hi");
        assertThat(ollamaService.received).isNull();
    }

    @Test
    void providerMatchIsCaseInsensitiveAndTrimmed() {
        startWith("  Bedrock-Claude  ");

        assertThat(router.getActiveProvider()).isEqualTo("bedrock");
    }

    @Test
    void unknownProviderFallsBackToOllama() {
        startWith("some-other-llm");

        assertThat(router.getActiveProvider()).isEqualTo("ollama");
    }

    @Test
    void nullProviderFallsBackToOllama() {
        startWith(null);

        assertThat(router.getActiveProvider()).isEqualTo("ollama");
    }

    @Test
    void setProviderSwitchesAtRuntimeAndReturnsNormalizedValue() {
        startWith("ollama-mistral");

        String applied = router.setProvider("bedrock");

        assertThat(applied).isEqualTo("bedrock");
        assertThat(router.getActiveProvider()).isEqualTo("bedrock");
        assertThat(router.generate("hi")).isEqualTo("bedrock-reply");
        assertThat(bedrockService.received).isEqualTo("hi");
    }

    @Test
    void setProviderCanSwitchBackToOllama() {
        startWith("bedrock");

        router.setProvider("ollama");

        assertThat(router.getActiveProvider()).isEqualTo("ollama");
        assertThat(router.generate("hi")).isEqualTo("ollama-reply");
        assertThat(ollamaService.received).isEqualTo("hi");
    }
}
