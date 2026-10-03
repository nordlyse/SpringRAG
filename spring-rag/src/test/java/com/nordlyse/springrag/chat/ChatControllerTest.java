package com.nordlyse.springrag.chat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(RagChatConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void answerStreamsTheModelReplyWithRetrievedContext() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("The stored note mentions a river.")));
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("A short note."))))));

        MvcResult result = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"What is in the documents?\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("A short note.");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText()).contains("The stored note mentions a river.");
    }

    @Test
    void answerCompletesWhenTheModelReturnsNoChunks() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.empty());

        MvcResult result = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

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

    @Test
    void answerRemembersAnEarlierTurnInTheSameConversation() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(
                Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Noted."))))),
                Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Jakob"))))));

        dispatch("{\"message\":\"My name is Jakob\",\"conversationId\":\"session-1\"}");
        dispatch("{\"message\":\"What is my name?\",\"conversationId\":\"session-1\"}");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).stream(prompt.capture());
        assertThat(prompt.getAllValues().get(1).getUserMessage().getText()).isEqualTo("What is my name?");
        assertThat(texts(prompt.getAllValues().get(1))).anyMatch(text -> text.contains("My name is Jakob"));
        assertThat(texts(prompt.getAllValues().get(1))).anyMatch(text -> text.contains("Noted."));
        assertThat(texts(prompt.getAllValues().get(1))).noneMatch(text -> text.contains("Context information"));
    }

    @Test
    void answerKeepsADifferentConversationSeparate() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Noted."))))));

        dispatch("{\"message\":\"My name is Jakob\",\"conversationId\":\"session-1\"}");
        dispatch("{\"message\":\"Hello\",\"conversationId\":\"session-2\"}");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).stream(prompt.capture());
        assertThat(texts(prompt.getAllValues().get(1))).noneMatch(text -> text.contains("Jakob"));
    }

    @Test
    void answerRejectsAConversationIdWithSpaces() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"Hello\",\"conversationId\":\"bad id\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatModel);
    }

    private void dispatch(String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(json))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
    }

    private static java.util.List<String> texts(Prompt prompt) {
        return prompt.getInstructions().stream().map(Message::getText).toList();
    }
}
