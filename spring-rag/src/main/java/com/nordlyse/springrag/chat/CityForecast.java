package com.nordlyse.springrag.chat;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @Autowired)
class CityForecast {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<String> DAILY = List.of(
            "temperature_2m_max",
            "temperature_2m_min",
            "precipitation_sum",
            "weather_code");

    private ObjectProvider<List<McpSyncClient>> clients;

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
            ZoneId zone = zone(match);
            LocalDate today = LocalDate.now(zone);
            WeatherWhen.Asked asked = WeatherWhen.resolve(question, today);
            boolean turkish = inTurkish(question);
            if (asked.getKind() == WeatherWhen.Kind.UNREADABLE || asked.getKind() == WeatherWhen.Kind.OUTSIDE) {
                return spokenUnavailable(placeName(match), asked, today, turkish);
            }
            boolean now = asked.getKind() == WeatherWhen.Kind.FORECAST && asked.getDate().equals(today);
            String body = text(client.callTool(new McpSchema.CallToolRequest(
                    toolFor(asked.getKind()),
                    arguments(match.path("latitude").asDouble(), match.path("longitude").asDouble(), asked.getDate(), now))));
            if (body.isBlank()) {
                return "";
            }
            return spoken(match, body, asked.getDate(), now, turkish);
        }
        catch (RuntimeException ignored) {
            return "";
        }
    }

    static String toolFor(WeatherWhen.Kind kind) {
        return kind == WeatherWhen.Kind.ARCHIVE ? "weather_archive" : "weather_forecast";
    }

    static Map<String, Object> arguments(double latitude, double longitude, LocalDate date, boolean now) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("latitude", latitude);
        args.put("longitude", longitude);
        args.put("timezone", "auto");
        args.put("start_date", date.toString());
        args.put("end_date", date.toString());
        args.put("daily", DAILY);
        if (now) {
            args.put("current", List.of("temperature_2m", "weather_code", "wind_speed_10m"));
        }
        return args;
    }

    static String summary(JsonNode place, String forecastJson, LocalDate date, String label, boolean now) {
        JsonNode forecast = JSON.readTree(forecastJson);
        JsonNode daily = forecast.path("daily");
        String where = where(place);
        String when = askedLine(date, label);
        JsonNode high = daily.path("temperature_2m_max").path(0);
        if (high.isMissingNode() || high.isNull()) {
            return """
                    Open-Meteo weather for %s.
                    Asked date: %s.
                    No daily values were returned for that date.
                    Do not invent a temperature.
                    """.formatted(where, when);
        }
        StringBuilder report = new StringBuilder();
        report.append("Open-Meteo weather for ").append(where).append(".\n");
        report.append("Asked date: ").append(when).append(".\n");
        if (now) {
            JsonNode current = forecast.path("current");
            report.append("Now: ")
                    .append(number(current.path("temperature_2m")))
                    .append("°C, ")
                    .append(sky(current.path("weather_code").asInt(-1)))
                    .append(", wind ")
                    .append(number(current.path("wind_speed_10m")))
                    .append(" km/h.\n");
        }
        report.append("Sky: ")
                .append(sky(daily.path("weather_code").path(0).asInt(-1)))
                .append(". High ")
                .append(number(high))
                .append("°C, low ")
                .append(number(daily.path("temperature_2m_min").path(0)))
                .append("°C, precipitation ")
                .append(number(daily.path("precipitation_sum").path(0)))
                .append(" mm.\n");
        return report.toString();
    }

    static String spoken(JsonNode place, String forecastJson, LocalDate date, boolean now, boolean turkish) {
        JsonNode forecast = JSON.readTree(forecastJson);
        JsonNode daily = forecast.path("daily");
        String where = placeName(place);
        String when = turkish ? turkishDate(date) : englishDate(date);
        JsonNode high = daily.path("temperature_2m_max").path(0);
        if (high.isMissingNode() || high.isNull()) {
            return turkish
                    ? when + " tarihinde " + where + " için günlük hava verisi yok."
                    : "No daily weather for " + where + " on " + when + ".";
        }
        int code = daily.path("weather_code").path(0).asInt(-1);
        String highText = number(high);
        String lowText = number(daily.path("temperature_2m_min").path(0));
        String rainText = number(daily.path("precipitation_sum").path(0));
        if (turkish) {
            StringBuilder answer = new StringBuilder();
            if (now) {
                JsonNode current = forecast.path("current");
                answer.append("Şu an ")
                        .append(where)
                        .append(" ")
                        .append(number(current.path("temperature_2m")))
                        .append("°C, ")
                        .append(skyTr(current.path("weather_code").asInt(-1), false))
                        .append(", rüzgar ")
                        .append(number(current.path("wind_speed_10m")))
                        .append(" km/sa. ");
            }
            answer.append(when)
                    .append(" tarihinde ")
                    .append(where)
                    .append(" için hava ")
                    .append(skyTr(code, true))
                    .append(". En yüksek sıcaklık ")
                    .append(highText)
                    .append("°C, en düşük ")
                    .append(lowText)
                    .append("°C, yağış ")
                    .append(rainText)
                    .append(" mm. Kaynak Open-Meteo.");
            return answer.toString();
        }
        StringBuilder answer = new StringBuilder();
        if (now) {
            JsonNode current = forecast.path("current");
            answer.append("Now in ")
                    .append(where)
                    .append(" it is ")
                    .append(number(current.path("temperature_2m")))
                    .append("°C, ")
                    .append(sky(current.path("weather_code").asInt(-1)))
                    .append(", wind ")
                    .append(number(current.path("wind_speed_10m")))
                    .append(" km/h. ");
        }
        answer.append("On ")
                .append(when)
                .append(" the weather in ")
                .append(where)
                .append(" was ")
                .append(sky(code))
                .append(". The high was ")
                .append(highText)
                .append("°C, the low ")
                .append(lowText)
                .append("°C, and precipitation ")
                .append(rainText)
                .append(" mm, from Open-Meteo.");
        return answer.toString();
    }

    static String spokenUnavailable(String where, WeatherWhen.Asked asked, LocalDate today, boolean turkish) {
        if (asked.getKind() == WeatherWhen.Kind.UNREADABLE) {
            return turkish
                    ? where + " için sorudaki tarih okunamadı."
                    : "The date in the question for " + where + " could not be read.";
        }
        if (asked.getDate().isBefore(WeatherWhen.ARCHIVE_START)) {
            return turkish
                    ? "Open-Meteo arşivi 1 Ocak 1940 tarihinde başlar. " + turkishDate(asked.getDate()) + " için veri yok."
                    : "The Open-Meteo archive starts on 1 January 1940. No weather for " + englishDate(asked.getDate()) + ".";
        }
        LocalDate last = today.plusDays(WeatherWhen.FORECAST_FUTURE_DAYS);
        return turkish
                ? turkishDate(asked.getDate()) + " için günlük tahmin yok. Günlük tahmin " + turkishDate(last) + " tarihine kadar."
                : "No daily forecast for " + englishDate(asked.getDate()) + ". Daily forecast runs through " + englishDate(last) + ".";
    }

    static String missed(boolean turkish) {
        return turkish
                ? "Bu soru için Open-Meteo hava verisi gelmedi."
                : "Open-Meteo returned no weather for this question.";
    }

    static String unavailable(String where, WeatherWhen.Asked asked, LocalDate today) {
        if (asked.getKind() == WeatherWhen.Kind.UNREADABLE) {
            return """
                    Open-Meteo weather for %s.
                    The date in the question could not be read.
                    Do not answer with today's weather.
                    Do not invent a temperature.
                    """.formatted(where);
        }
        if (asked.getDate().isBefore(WeatherWhen.ARCHIVE_START)) {
            return """
                    Open-Meteo weather for %s.
                    Asked date: %s.
                    The Open-Meteo archive starts on 1940-01-01.
                    Do not invent a temperature.
                    """.formatted(where, asked.getDate());
        }
        return """
                Open-Meteo weather for %s.
                Asked date: %s.
                No daily forecast is available that far ahead.
                Daily forecast runs through %s.
                Do not invent a temperature.
                """.formatted(where, askedLine(asked.getDate(), asked.getLabel()), today.plusDays(WeatherWhen.FORECAST_FUTURE_DAYS));
    }

    static boolean inTurkish(String question) {
        String lower = question.toLowerCase(java.util.Locale.forLanguageTag("tr"));
        return lower.contains("hava")
                || lower.contains("nasıl")
                || lower.contains("nasil")
                || lower.contains("gün")
                || lower.contains("gun");
    }

    private static String placeName(JsonNode place) {
        String name = place.path("name").asString();
        if (name.endsWith(" Province")) {
            name = name.substring(0, name.length() - " Province".length());
        }
        return name.isBlank() ? "the city" : name;
    }

    private static String turkishDate(LocalDate date) {
        String[] months = {
                "", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        };
        return date.getDayOfMonth() + " " + months[date.getMonthValue()] + " " + date.getYear();
    }

    private static String englishDate(LocalDate date) {
        String[] months = {
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
        return date.getDayOfMonth() + " " + months[date.getMonthValue()] + " " + date.getYear();
    }

    private static String skyTr(int code, boolean past) {
        String word = switch (code) {
            case 0 -> "açık";
            case 1 -> "az bulutlu";
            case 2 -> "parçalı bulutlu";
            case 3 -> "çok bulutlu";
            case 45, 48 -> "sisli";
            case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "yağmurlu";
            case 71, 73, 75, 77, 85, 86 -> "karlı";
            case 95, 96, 99 -> "gök gürültülü";
            default -> "belirsiz";
        };
        if (!past) {
            return word;
        }
        return switch (word) {
            case "açık" -> "açıktı";
            case "sisli" -> "sisliydi";
            case "yağmurlu" -> "yağmurluydu";
            case "karlı" -> "karlıydı";
            case "belirsiz" -> "belirsizdi";
            default -> word + "ydu";
        };
    }

    private static String where(JsonNode place) {
        String name = place.path("name").asString();
        String country = place.path("country").asString();
        return country.isBlank() ? name : name + ", " + country;
    }

    private static String askedLine(LocalDate date, String label) {
        if (label == null || label.isBlank() || label.equals(date.toString())) {
            return date.toString();
        }
        return date + " (" + label + ")";
    }

    private static ZoneId zone(JsonNode place) {
        String name = place.path("timezone").asString("");
        if (name.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(name);
        }
        catch (DateTimeException ignored) {
            return ZoneId.of("UTC");
        }
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
