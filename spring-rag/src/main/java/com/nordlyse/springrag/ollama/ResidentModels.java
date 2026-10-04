package com.nordlyse.springrag.ollama;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring-rag.keep-models-loaded", havingValue = "true", matchIfMissing = true)
public class ResidentModels implements SmartLifecycle {

    static final String HOLD = "-1s";
    static final String RELEASE = "0";

    private static final Logger log = LoggerFactory.getLogger(ResidentModels.class);

    private final OllamaApi ollamaApi;
    private final String chatModel;
    private final String embeddingModel;
    private volatile boolean running;
    private boolean chatHeld;
    private boolean embeddingHeld;

    ResidentModels(
            OllamaApi ollamaApi,
            @Value("${spring.ai.ollama.chat.model:llama3.2}") String chatModel,
            @Value("${spring.ai.ollama.embedding.model:nomic-embed-text}") String embeddingModel) {
        this.ollamaApi = ollamaApi;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        log.info("Holding {} and {} in memory until the application stops.", chatModel, embeddingModel);
        ollamaApi.embed(embeddingRequest(HOLD));
        embeddingHeld = true;
        ollamaApi.chat(chatRequest(HOLD));
        chatHeld = true;
        running = true;
    }

    @Override
    public void stop() {
        if (chatHeld) {
            ollamaApi.chat(chatRequest(RELEASE));
            chatHeld = false;
        }
        if (embeddingHeld) {
            ollamaApi.embed(embeddingRequest(RELEASE));
            embeddingHeld = false;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    private OllamaApi.ChatRequest chatRequest(String keepAlive) {
        return OllamaApi.ChatRequest.builder(chatModel)
                .stream(false)
                .keepAlive(keepAlive)
                .build();
    }

    private OllamaApi.EmbeddingsRequest embeddingRequest(String keepAlive) {
        return new OllamaApi.EmbeddingsRequest(embeddingModel, List.of("ok"), keepAlive, null, null, null);
    }
}
