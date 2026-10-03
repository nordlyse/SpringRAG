package com.nordlyse.springrag.chat;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagChatConfig {

    static final int MEMORY_WINDOW = 20;

    private static final String ANSWER_PROMPT = """
            {query}

            Context information is below.
            ---------------------
            {question_answer_context}
            ---------------------

            Answer from the conversation so far and from the context above.
            Facts the user already stated in this conversation remain available for later questions.
            When the context has document passages, use them for questions about those documents.
            When the context is empty, answer from the conversation.
            """;

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(MEMORY_WINDOW).build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(SearchRequest.DEFAULT_TOP_K)
                .similarityThresholdAll()
                .build();
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(new PromptTemplate(ANSWER_PROMPT))
                .build();
    }
}
