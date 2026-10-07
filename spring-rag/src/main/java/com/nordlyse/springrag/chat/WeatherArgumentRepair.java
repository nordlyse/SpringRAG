package com.nordlyse.springrag.chat;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
final class WeatherArgumentRepair {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Map<String, String> COUNTRY_CODES = Map.ofEntries(
            Map.entry("turkey", "TR"),
            Map.entry("turkiye", "TR"),
            Map.entry("türkiye", "TR"),
            Map.entry("norway", "NO"),
            Map.entry("norge", "NO"),
            Map.entry("germany", "DE"),
            Map.entry("deutschland", "DE"),
            Map.entry("almanya", "DE"),
            Map.entry("france", "FR"),
            Map.entry("fransa", "FR"),
            Map.entry("sweden", "SE"),
            Map.entry("sverige", "SE"),
            Map.entry("denmark", "DK"),
            Map.entry("danmark", "DK"),
            Map.entry("finland", "FI"),
            Map.entry("united states", "US"),
            Map.entry("usa", "US"),
            Map.entry("united kingdom", "GB"),
            Map.entry("uk", "GB"));

    private static final Set<String> INTEGER_ARGUMENTS = Set.of(
            "count", "forecast_days", "past_days", "past_hours", "forecast_hours", "tilt", "azimuth");

    private static final Set<String> NUMBER_ARGUMENTS = Set.of("latitude", "longitude");

    private static final Set<String> LIST_ARGUMENTS = Set.of("hourly", "daily", "minutely_15", "current", "models");

    static String repair(String toolName, String toolInput) {
        if (toolInput == null || toolInput.isBlank() || toolName == null) {
            return toolInput;
        }
        if (!"geocoding".equals(toolName) && !"weather_forecast".equals(toolName)) {
            return toolInput;
        }
        try {
            JsonNode parsed = JSON.readTree(toolInput);
            if (!(parsed instanceof ObjectNode arguments)) {
                return toolInput;
            }
            if ("geocoding".equals(toolName)) {
                repairGeocoding(arguments);
            }
            else {
                repairForecast(arguments);
            }
            return JSON.writeValueAsString(arguments);
        }
        catch (JacksonException ignored) {
            return toolInput;
        }
    }

    private static void repairGeocoding(ObjectNode arguments) {
        copyPlaceName(arguments);
        repairInteger(arguments, "count", 1, 100);
        repairCountryCode(arguments);
        repairLanguage(arguments);
        if (arguments.has("format") && !"json".equals(arguments.path("format").asString())) {
            arguments.remove("format");
        }
    }

    private static void repairForecast(ObjectNode arguments) {
        for (String name : NUMBER_ARGUMENTS) {
            repairNumber(arguments, name);
        }
        for (String name : INTEGER_ARGUMENTS) {
            repairInteger(arguments, name, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        for (String name : LIST_ARGUMENTS) {
            repairList(arguments, name);
        }
    }

    private static void copyPlaceName(ObjectNode arguments) {
        if (arguments.path("name").asString().length() >= 2) {
            return;
        }
        for (String alias : new String[] {"city", "place", "query", "location"}) {
            String value = arguments.path(alias).asString().trim();
            if (value.length() >= 2) {
                arguments.put("name", value);
                arguments.remove(alias);
                return;
            }
        }
    }

    private static void repairCountryCode(ObjectNode arguments) {
        if (!arguments.has("countryCode") || arguments.path("countryCode").isNull()) {
            return;
        }
        String code = countryCode(arguments.path("countryCode").asString());
        if (code == null) {
            arguments.remove("countryCode");
        }
        else {
            arguments.put("countryCode", code);
        }
    }

    private static String countryCode(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.matches("[A-Za-z]{2}")) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return COUNTRY_CODES.get(trimmed.toLowerCase(Locale.ROOT));
    }

    private static void repairLanguage(ObjectNode arguments) {
        if (!arguments.has("language")) {
            return;
        }
        String language = arguments.path("language").asString().trim().toLowerCase(Locale.ROOT);
        if (!language.matches("[a-z]{2}")) {
            arguments.remove("language");
        }
        else {
            arguments.put("language", language);
        }
    }

    private static void repairInteger(ObjectNode arguments, String name, int minimum, int maximum) {
        if (!arguments.has(name)) {
            return;
        }
        Integer value = integer(arguments.get(name));
        if (value == null || value < minimum || value > maximum) {
            arguments.remove(name);
        }
        else {
            arguments.put(name, value);
        }
    }

    private static void repairNumber(ObjectNode arguments, String name) {
        if (!arguments.has(name)) {
            return;
        }
        Double value = number(arguments.get(name));
        if (value == null) {
            arguments.remove(name);
        }
        else {
            arguments.put(name, value);
        }
    }

    private static void repairList(ObjectNode arguments, String name) {
        if (!arguments.has(name) || arguments.get(name) instanceof ArrayNode) {
            return;
        }
        JsonNode value = arguments.get(name);
        if (!value.isString()) {
            arguments.remove(name);
            return;
        }
        ArrayNode items = JSON.createArrayNode();
        for (String part : value.asString().split(",")) {
            String item = part.trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            arguments.remove(name);
        }
        else {
            arguments.set(name, items);
        }
    }

    private static Integer integer(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.intValue();
        }
        Double value = number(node);
        if (value == null || value != Math.rint(value)) {
            return null;
        }
        return value.intValue();
    }

    private static Double number(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.doubleValue();
        }
        if (!node.isString()) {
            return null;
        }
        try {
            return Double.valueOf(node.asString().trim());
        }
        catch (NumberFormatException ignored) {
            return null;
        }
    }
}
