package com.nordlyse.springrag.chat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void answerStreamsTheModelReplyWhenNoPassageMatches() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                chunk("A "),
                chunk("short "),
                chunk("note.")));

        String body = dispatch("{\"message\":\"Where did I work?\"}");

        assertThat(body).contains("A ");
        assertThat(body).contains("short ");
        assertThat(body).contains("note.");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText()).contains("Where did I work?");
        assertThat(prompt.getValue().getUserMessage().getText()).contains("general knowledge");
    }

    @Test
    void answerUsesMatchingPassagesBeforeTheModel() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(
                        new Document("Certificate 2025 only."),
                        new Document("Systemutvikler, Trondheim, Aug.2020 – Feb.2025. Worked there.")));
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk("Trondheim.")));

        String body = dispatch("{\"message\":\"Where did I work?\"}");

        assertThat(body).contains("Trondheim.");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(prompt.capture());
        String text = prompt.getValue().getUserMessage().getText();
        assertThat(text).contains("Trondheim");
        assertThat(text).contains("employer and place");
        assertThat(text).doesNotContain("Certificate 2025");
        assertThat(text).doesNotContain("general knowledge");
    }

    @Test
    void answerCompletesWhenTheModelReturnsNoChunks() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.empty());

        String body = dispatch("{\"message\":\"hello\"}");

        assertThat(body).doesNotContain("data:");
    }

    @Test
    void answerRejectsABlankMessage() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatModel);
        verifyNoInteractions(vectorStore);
    }

    private String dispatch(String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(json))
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
