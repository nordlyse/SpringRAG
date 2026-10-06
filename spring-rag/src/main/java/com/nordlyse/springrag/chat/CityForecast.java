package com.nordlyse.springrag.chat;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
class CityForecast {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ObjectProvider<List<McpSyncClient>> clients;

    CityForecast(ObjectProvider<List<McpSyncClient>> clients) {
        this.clients = clients;
    }

    String report(String question) {
        List<McpSyncClient> found = clients.getIfAvailable();
        if (found == null || found.isEmpty()) {
            return "";
        }
        String city = PassageQuestion.city(question);
        if (city.isBlank()) {
            return "";
        }
        try {
            McpSyncClient client = found.get(0);
            String place = text(client.callTool(new McpSchema.CallToolRequest(
                    "geocoding",
                    Map.of("name", city, "count", 1))));
            JsonNode match = firstPlace(place);
            if (match == null) {
                return "";
            }
            double latitude = match.path("latitude").asDouble();
            double longitude = match.path("longitude").asDouble();
            String forecast = text(client.callTool(new McpSchema.CallToolRequest(
                    "weather_forecast",
                    Map.of(
                            "latitude", latitude,
                            "longitude", longitude,
                            "timezone", "auto",
                            "forecast_days", 1,
                            "current", List.of("temperature_2m", "weather_code", "wind_speed_10m"),
                            "daily", List.of(
                                    "temperature_2m_max",
                                    "temperature_2m_min",
                                    "precipitation_sum",
                                    "weather_code")))));
            return summary(match, forecast);
        }
        catch (RuntimeException ignored) {
            return "";
        }
    }

    static String summary(JsonNode place, String forecastJson) {
        JsonNode forecast = JSON.readTree(forecastJson);
        JsonNode current = forecast.path("current");
        JsonNode daily = forecast.path("daily");
        String name = place.path("name").asString();
        String country = place.path("country").asString();
        String where = country.isBlank() ? name : name + ", " + country;
        return """
                Open-Meteo weather for %s.
                Now: %s°C, %s, wind %s km/h.
                Today: high %s°C, low %s°C, precipitation %s mm.
                """.formatted(
                where,
                number(current.path("temperature_2m")),
                sky(current.path("weather_code").asInt(-1)),
                number(current.path("wind_speed_10m")),
                number(daily.path("temperature_2m_max").path(0)),
                number(daily.path("temperature_2m_min").path(0)),
                number(daily.path("precipitation_sum").path(0)));
    }

    private static JsonNode firstPlace(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode results = JSON.readTree(body).path("results");
        if (!results.isArray() || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private static String text(McpSchema.CallToolResult result) {
        if (result == null || Boolean.TRUE.equals(result.isError()) || result.content() == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent text) {
                body.append(text.text());
            }
        }
        return body.toString();
    }

    private static String number(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "?";
        }
        return node.asString();
    }

    private static String sky(int code) {
        return switch (code) {
            case 0 -> "clear";
            case 1 -> "mainly clear";
            case 2 -> "partly cloudy";
            case 3 -> "overcast";
            case 45, 48 -> "fog";
            case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "rain";
            case 71, 73, 75, 77, 85, 86 -> "snow";
            case 95, 96, 99 -> "thunderstorm";
            default -> "unspecified sky";
        };
    }
}
