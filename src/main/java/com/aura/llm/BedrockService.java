package com.aura.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SpecificToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

import java.util.List;

/**
 * Calls Claude Sonnet 4.6 through the Amazon Bedrock Converse API.
 *
 * <p>Structured JSON output is obtained by declaring a single tool whose
 * {@code inputSchema} is the JSON shape we want back, and forcing the model to
 * call it via {@code toolChoice}. Claude then replies with a {@code toolUse}
 * block whose {@code input} is JSON guaranteed to match the schema.</p>
 *
 * <p>The {@link BedrockRuntimeClient} is created lazily on first use, so when
 * {@code llm.provider} is left on Ollama this service never resolves AWS
 * credentials or opens a connection — the existing Ollama path is untouched.</p>
 */
@Service
public class BedrockService implements LlmService {

    /** AWS region hosting the Bedrock model. */
    @Value("${bedrock.region:us-east-1}")
    private String region;

    /**
     * Bedrock model id for Claude Sonnet 4.6. Current-generation Claude models
     * on Bedrock are served through cross-region inference profiles, so the id
     * carries a regional prefix (e.g. {@code us.}, {@code eu.}, {@code apac.}).
     * Override in application.properties if your account uses a different one.
     */
    @Value("${bedrock.model.id:us.anthropic.claude-sonnet-4-6}")
    private String modelId;

    /** Upper bound on output tokens for a single Converse call. */
    @Value("${bedrock.max.tokens:2000}")
    private int maxTokens;

    private static final String TOOL_NAME = "structured_response";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Created on first use (double-checked locking) so Ollama-only runs stay AWS-free.
    private volatile BedrockRuntimeClient client;

    private BedrockRuntimeClient client() {
        BedrockRuntimeClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    // Uses the default AWS credential provider chain
                    // (env vars, ~/.aws/credentials, instance/role profile, ...).
                    local = BedrockRuntimeClient.builder()
                            .region(Region.of(region))
                            .build();
                    client = local;
                }
            }
        }
        return local;
    }

    @Override
    public String generate(String promptText) {
        try {
            Message userMessage = Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromText(promptText))
                    .build();

            ConverseResponse response = client().converse(request -> request
                    .modelId(modelId)
                    .messages(userMessage)
                    .toolConfig(structuredOutputToolConfig())
                    .inferenceConfig(cfg -> cfg.maxTokens(maxTokens)));

            // With a forced tool, Claude answers with a toolUse block whose
            // input is the structured JSON matching our schema.
            for (ContentBlock block : response.output().message().content()) {
                if (block.toolUse() != null) {
                    Object json = block.toolUse().input().unwrap();
                    return objectMapper.writeValueAsString(json);
                }
            }

            // Fallback: surface any plain text the model returned instead.
            return response.output().message().content().stream()
                    .map(ContentBlock::text)
                    .filter(text -> text != null)
                    .findFirst()
                    .orElse("No structured output returned from Bedrock.");

        } catch (Exception e) {
            e.printStackTrace();
            return "Bedrock Error: " + e.getMessage();
        }
    }

    /**
     * Defines the structured-output tool and forces the model to use it.
     * The JSON schema here is the exact shape Claude must return.
     */
    private ToolConfiguration structuredOutputToolConfig() {
        Document schema = Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("answer", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description", "The complete answer to the user's prompt.")
                                .build())
                        .putDocument("key_points", Document.mapBuilder()
                                .putString("type", "array")
                                .putString("description", "Key supporting points, if any.")
                                .putDocument("items", Document.mapBuilder()
                                        .putString("type", "string")
                                        .build())
                                .build())
                        .putDocument("sentiment", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description", "Overall sentiment of the answer: positive, neutral, or negative.")
                                .build())
                        .build())
                .putDocument("required", Document.fromList(List.of(Document.fromString("answer"))))
                .build();

        ToolSpecification toolSpec = ToolSpecification.builder()
                .name(TOOL_NAME)
                .description("Return the assistant's reply to the user as structured JSON.")
                .inputSchema(ToolInputSchema.fromJson(schema))
                .build();

        return ToolConfiguration.builder()
                .tools(Tool.fromToolSpec(toolSpec))
                .toolChoice(ToolChoice.fromTool(SpecificToolChoice.builder().name(TOOL_NAME).build()))
                .build();
    }
}
