package com.nordlyse.springrag.chat;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private static final int PASSAGE_LIMIT = 20;

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final double similarityThreshold;

    public ChatController(
            ChatModel chatModel,
            VectorStore vectorStore,
            @Value("${spring-rag.similarity-threshold:0.4}") double similarityThreshold) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> answer(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required.");
        }
        List<Document> passages = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.message())
                .topK(PASSAGE_LIMIT)
                .similarityThreshold(similarityThreshold)
                .build());
        String prompt = PassageQuestion.prompt(request.message(), passages);
        return chatModel.stream(new Prompt(prompt))
                .map(ChatController::chunk)
                .filter(text -> !text.isEmpty());
    }

    private static String chunk(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }
}
