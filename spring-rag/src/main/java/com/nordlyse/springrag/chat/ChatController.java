package com.nordlyse.springrag.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nordlyse.springrag.user.UserDirectory;

import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatController(
            StreamingChatModel streamingChatModel,
            ChatMemory chatMemory,
            DocumentPassageAdvisor documentPassageAdvisor,
            UserDirectory userDirectory,
            ObjectProvider<ToolCallbackProvider> weatherTools) {
        this.chatMemory = chatMemory;
        ChatClient.Builder chat = ChatClient.builder(chatModel(streamingChatModel))
                .defaultSystem(RagChatConfig.CONVERSATION_SYSTEM)
                .defaultAdvisors(documentPassageAdvisor)
                .defaultTools(userDirectory);
        weatherTools.ifAvailable(tools -> chat.defaultTools(tools));
        this.chatClient = chat.build();
    }

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> answer(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required.");
        }
        String conversationId = conversationId(request.conversationId());
        chatMemory.add(conversationId, new UserMessage(request.message()));
        List<Message> earlier = earlierMessages(chatMemory.get(conversationId));
        StringBuilder reply = new StringBuilder();
        return chatClient.prompt()
                .messages(earlier)
                .user(request.message())
                .stream()
                .content()
                .doOnNext(reply::append)
                .doOnComplete(() -> chatMemory.add(conversationId, new AssistantMessage(reply.toString())));
    }

    private static List<Message> earlierMessages(List<Message> history) {
        if (history.size() < 2) {
            return List.of();
        }
        return new ArrayList<>(history.subList(0, history.size() - 1));
    }

    private static String conversationId(String requestedId) {
        if (requestedId == null || requestedId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String conversationId = requestedId.trim();
        if (conversationId.length() > 80 || !conversationId.matches("[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation id is not allowed.");
        }
        return conversationId;
    }

    private static ChatModel chatModel(StreamingChatModel streamingChatModel) {
        if (streamingChatModel instanceof ChatModel model) {
            return model;
        }
        throw new IllegalStateException("Streaming chat model cannot answer a prompt.");
    }
}
