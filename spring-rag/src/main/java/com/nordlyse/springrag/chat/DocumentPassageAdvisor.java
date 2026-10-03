package com.nordlyse.springrag.chat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class DocumentPassageAdvisor implements BaseAdvisor {

    private static final String PASSAGE_PROMPT = """
            {query}

            Answer from the conversation when the question is about a fact the user already stated.
            Use the document passages below only when the question is about those documents.
            Give a short answer in the user's language.

            ---------------------
            {question_answer_context}
            ---------------------
            """;

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final PromptTemplate promptTemplate;

    DocumentPassageAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.searchRequest = SearchRequest.builder()
                .topK(SearchRequest.DEFAULT_TOP_K)
                .similarityThresholdAll()
                .build();
        this.promptTemplate = new PromptTemplate(PASSAGE_PROMPT);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String query = request.prompt().getUserMessage().getText();
        String searchText = query == null ? "" : query;
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.from(searchRequest).query(searchText).build());
        if (documents == null || documents.isEmpty()) {
            return request;
        }
        String passages = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));
        String rendered = promptTemplate.render(Map.of(
                "query", searchText,
                "question_answer_context", passages));
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
