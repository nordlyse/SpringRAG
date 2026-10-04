package com.nordlyse.springrag.chat;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class DocumentPassageAdvisor implements BaseAdvisor {

    private static final int PASSAGE_LIMIT = 20;

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    DocumentPassageAdvisor(VectorStore vectorStore, double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String query = request.prompt().getUserMessage().getText();
        String searchText = query == null ? "" : query;
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(searchText)
                .topK(PASSAGE_LIMIT)
                .similarityThreshold(similarityThreshold)
                .build());
        if (documents == null || documents.isEmpty()) {
            return request;
        }
        String rendered = PassageQuestion.prompt(searchText, documents);
        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(rendered))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
