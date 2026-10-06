package com.nordlyse.springrag.chat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.nordlyse.springrag.user.UserDirectory;
import com.nordlyse.springrag.user.UserStore;
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
@Import({RagChatConfig.class, UserDirectory.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    @SuppressWarnings("unused")
    private UserStore userStore;

    @MockitoBean
    private ToolCallbackProvider weatherTools;

    @BeforeEach
    void noWeatherTools() {
        when(weatherTools.getToolCallbacks()).thenReturn(new org.springframework.ai.tool.ToolCallback[0]);
    }

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
        assertThat(((ToolCallingChatOptions) prompt.getValue().getOptions()).getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .contains("findUser", "rolesForUser");
        assertThat(texts(prompt.getValue())).anyMatch(text -> text.contains("Do not invent a forecast"));
    }

    @Test
    void answerCanCallTheCityWeatherTools() throws Exception {
        when(weatherTools.getToolCallbacks()).thenReturn(ToolCallbacks.from(new CityWeather()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Mild."))))));

        dispatch("{\"message\":\"What is the weather in Ankara?\"}");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(prompt.capture());
        assertThat(((ToolCallingChatOptions) prompt.getValue().getOptions()).getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .contains("geocoding", "weather_forecast", "findUser", "rolesForUser");
    }

    @Test
    void answerUsesEmploymentLinesWhenTheQuestionAsksForWorkplaces() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(
                        new Document("Certificate 2025 only."),
                        new Document("Systemutvikler, Trondheim, Aug.2020 – Feb.2025. Worked there.")));
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Trondheim."))))));

        dispatch("{\"message\":\"Where did I work?\"}");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(prompt.capture());
        String text = prompt.getValue().getUserMessage().getText();
        assertThat(text).contains("Trondheim");
        assertThat(text).contains("employer and place");
        assertThat(text).doesNotContain("Certificate 2025");
    }

    @Test
    void answerKeepsAnotherPersonsWorkplacesOutWhenTheQuestionNamesSomeone() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0);
            if ("hilal".equalsIgnoreCase(request.getQuery())) {
                return List.of(Document.builder()
                        .text("Hilal Demir")
                        .metadata("file_name", "hilal.pdf")
                        .build());
            }
            if (request.hasFilterExpression()) {
                return List.of(Document.builder()
                        .text("Systemutvikler – Nordlyse, Oslo, Aug.2021 – Feb.2024. Worked there.")
                        .metadata("file_name", "hilal.pdf")
                        .build());
            }
            return List.of(
                    Document.builder()
                            .text("Systemutvikler, Trondheim, Aug.2020 – Feb.2025. Worked there.")
                            .metadata("file_name", "jakob.pdf")
                            .build(),
                    Document.builder()
                            .text("Systemutvikler – Nordlyse, Oslo, Aug.2021 – Feb.2024. Worked there.")
                            .metadata("file_name", "hilal.pdf")
                            .build());
        });
        when(chatModel.getOptions()).thenReturn(OllamaChatOptions.builder().build());
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Oslo."))))));

        dispatch("{\"message\":\"Ben Hilalim, nerede calistim?\"}");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(prompt.capture());
        String text = prompt.getValue().getUserMessage().getText();
        assertThat(text).contains("Oslo");
        assertThat(text).contains("Hilal");
        assertThat(text).doesNotContain("Trondheim");
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

    static final class CityWeather {

        @Tool(name = "geocoding", description = "Find a city by name.")
        public String geocoding(String name) {
            return name;
        }

        @Tool(name = "weather_forecast", description = "Forecast for one pair of coordinates.")
        public String weatherForecast(double latitude, double longitude) {
            return latitude + "," + longitude;
        }
    }
}
