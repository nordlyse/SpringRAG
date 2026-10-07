package com.nordlyse.springrag.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class DocumentPassageAdvisor implements BaseAdvisor {

    private static final int PASSAGE_LIMIT = 20;

    private VectorStore vectorStore;
    private double similarityThreshold;
    private ObjectProvider<CityForecast> cityForecast;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String query = request.prompt().getUserMessage().getText();
        String searchText = query == null ? "" : query;
        if (PassageQuestion.asksForWeather(searchText)) {
            CityForecast forecast = cityForecast.getIfAvailable();
            if (forecast == null) {
                return request;
            }
            String report = forecast.report(searchText);
            if (report.isBlank()) {
                report = """
                        No Open-Meteo weather was found for this question.
                        Do not mention tools.
                        Do not invent a temperature.
                        """;
            }
            return request.mutate()
                    .prompt(request.prompt().augmentUserMessage(PassageQuestion.weatherPrompt(searchText, report)))
                    .build();
        }
        List<Document> documents = passagesFor(searchText);
        if (documents.isEmpty() && PassageQuestion.statedName(searchText) == null) {
            return request;
        }
        String rendered = PassageQuestion.prompt(searchText, documents);
        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(rendered))
                .build();
    }

    private List<Document> passagesFor(String message) {
        String name = PassageQuestion.statedName(message);
        if (name == null) {
            return search(message, null, similarityThreshold);
        }
        Set<String> files = PassageQuestion.filesAbout(name, search(name, null, 0));
        if (files.isEmpty()) {
            return List.of();
        }
        return search(message, files, similarityThreshold);
    }

    private List<Document> search(String query, Set<String> files, double threshold) {
        SearchRequest.Builder request = SearchRequest.builder()
                .query(query)
                .topK(PASSAGE_LIMIT)
                .similarityThreshold(threshold);
        if (files != null && !files.isEmpty()) {
            FilterExpressionBuilder filters = new FilterExpressionBuilder();
            if (files.size() == 1) {
                request.filterExpression(filters.eq(PassageQuestion.FILE_NAME, files.iterator().next()).build());
            }
            else {
                request.filterExpression(filters.in(PassageQuestion.FILE_NAME, new ArrayList<Object>(files)).build());
            }
        }
        List<Document> found = vectorStore.similaritySearch(request.build());
        return found == null ? List.of() : found;
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
