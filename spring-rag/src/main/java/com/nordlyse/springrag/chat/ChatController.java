package com.nordlyse.springrag.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(StreamingChatModel streamingChatModel, QuestionAnswerAdvisor questionAnswerAdvisor) {
        this.chatClient = ChatClient.builder(chatModel(streamingChatModel))
                .defaultAdvisors(questionAnswerAdvisor)
                .build();
    }

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> answer(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required.");
        }
        return chatClient.prompt().user(request.message()).stream().content();
    }

    private static ChatModel chatModel(StreamingChatModel streamingChatModel) {
        if (streamingChatModel instanceof ChatModel model) {
            return model;
        }
        throw new IllegalStateException("Streaming chat model cannot answer a prompt.");
    }
}
