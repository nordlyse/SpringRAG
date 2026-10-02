package com.nordlyse.springrag.chat;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagChatConfig {

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(SearchRequest.DEFAULT_TOP_K)
                .similarityThresholdAll()
                .build();
        return QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
    }
}
