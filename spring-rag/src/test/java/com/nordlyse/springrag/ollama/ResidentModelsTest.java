package com.nordlyse.springrag.ollama;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.ollama.api.OllamaApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ResidentModelsTest {

    @Test
    void startHoldsBothModelsUntilTheApplicationStops() {
        OllamaApi ollamaApi = mock(OllamaApi.class);
        ResidentModels models = new ResidentModels(ollamaApi, "llama3.2", "nomic-embed-text");

        models.start();
        assertThat(models.isRunning()).isTrue();

        models.stop();

        ArgumentCaptor<OllamaApi.ChatRequest> chat = ArgumentCaptor.forClass(OllamaApi.ChatRequest.class);
        ArgumentCaptor<OllamaApi.EmbeddingsRequest> embedding = ArgumentCaptor.forClass(OllamaApi.EmbeddingsRequest.class);
        verify(ollamaApi, times(2)).chat(chat.capture());
        verify(ollamaApi, times(2)).embed(embedding.capture());
        assertThat(chat.getAllValues()).extracting(OllamaApi.ChatRequest::model)
                .containsOnly("llama3.2");
        assertThat(chat.getAllValues()).extracting(OllamaApi.ChatRequest::keepAlive)
                .containsExactly(ResidentModels.HOLD, ResidentModels.RELEASE);
        assertThat(embedding.getAllValues()).extracting(OllamaApi.EmbeddingsRequest::model)
                .containsOnly("nomic-embed-text");
        assertThat(embedding.getAllValues()).extracting(OllamaApi.EmbeddingsRequest::keepAlive)
                .containsExactly(ResidentModels.HOLD, ResidentModels.RELEASE);
        assertThat(models.isRunning()).isFalse();
    }
}
