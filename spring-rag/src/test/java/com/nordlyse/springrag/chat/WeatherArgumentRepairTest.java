package com.nordlyse.springrag.chat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherArgumentRepairTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void geocodingAcceptsATextCountAndACountryName() {
        JsonNode repaired = json(WeatherArgumentRepair.repair(
                "geocoding",
                "{\"name\":\"Ankara\",\"count\":\"1\",\"countryCode\":\"Turkey\"}"));

        assertThat(repaired.path("count").isInt()).isTrue();
        assertThat(repaired.path("count").intValue()).isEqualTo(1);
        assertThat(repaired.path("countryCode").asString()).isEqualTo("TR");
        assertThat(repaired.path("name").asString()).isEqualTo("Ankara");
    }

    @Test
    void geocodingDropsACountryThatIsNotACode() {
        JsonNode repaired = json(WeatherArgumentRepair.repair(
                "geocoding",
                "{\"name\":\"Ankara\",\"countryCode\":\"capital\"}"));

        assertThat(repaired.has("countryCode")).isFalse();
        assertThat(repaired.path("name").asString()).isEqualTo("Ankara");
    }

    @Test
    void forecastCoordinatesArriveAsNumbers() {
        JsonNode repaired = json(WeatherArgumentRepair.repair(
                "weather_forecast",
                "{\"latitude\":\"39.93\",\"longitude\":\"32.86\",\"forecast_days\":\"1\",\"daily\":\"temperature_2m_max\"}"));

        assertThat(repaired.path("latitude").isNumber()).isTrue();
        assertThat(repaired.path("longitude").doubleValue()).isEqualTo(32.86);
        assertThat(repaired.path("forecast_days").intValue()).isEqualTo(1);
        assertThat(repaired.path("daily").get(0).asString()).isEqualTo("temperature_2m_max");
    }

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }
}
