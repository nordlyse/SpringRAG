package com.nordlyse.springrag.chat;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagChatConfig {

    static final int MEMORY_WINDOW = 20;

    static final String CONVERSATION_SYSTEM = """
            You are talking with one person across several turns.
            Facts they tell you, including their name, stay true later in the chat.
            When they ask for one of those facts, answer with that fact in one short sentence, in their language.
            Do not ask them to provide context, history, or documents for a fact they already stated.
            Use document passages only when the question is about those documents.
            """;

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(MEMORY_WINDOW).build();
    }

    @Bean
    DocumentPassageAdvisor documentPassageAdvisor(VectorStore vectorStore) {
        return new DocumentPassageAdvisor(vectorStore);
    }
}
