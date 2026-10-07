package com.nordlyse.springrag.chat;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@Getter
@Setter
@NoArgsConstructor
public class RagChatConfig {

    static final int MEMORY_WINDOW = 20;

    static final String CONVERSATION_SYSTEM = """
            You are talking with one person across several turns.
            Facts they tell you, including their name, stay true later in the chat.
            When they ask for one of those facts, answer with that fact in one short sentence, in their language.
            Do not ask them to provide context, history, or documents for a fact they already stated.
            Use document passages only when the question is about those documents.
            When they ask for a user by id or username, call findUser.
            When they ask which roles a user has, call rolesForUser.
            Answer from the tool result in their language.
            Do not invent a user or a role.
            When a weather report is included with the question, answer from that report in their language, in two short sentences.
            Name the asked date from the report.
            Do not call another day today.
            Do not mention tools.
            Do not invent a forecast, a temperature, or a coordinate.
            Say that the weather comes from Open-Meteo.
            """;

    static final String WEATHER_SYSTEM = """
            Answer the weather question from the Open-Meteo report included with it.
            Answer in the same language as the question, in two short sentences.
            Name the asked date from the report.
            Do not use any other date.
            If the report says the day is before today, do not say it is after today.
            Do not call another day today.
            Do not mention tools.
            Do not invent a forecast, a temperature, or a coordinate.
            Say that the weather comes from Open-Meteo.
            """;

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(MEMORY_WINDOW).build();
    }

    @Bean
    DocumentPassageAdvisor documentPassageAdvisor(
            VectorStore vectorStore,
            @Value("${spring-rag.similarity-threshold:0.32}") double similarityThreshold,
            ObjectProvider<CityForecast> cityForecast) {
        return new DocumentPassageAdvisor(vectorStore, similarityThreshold, cityForecast);
    }
}
