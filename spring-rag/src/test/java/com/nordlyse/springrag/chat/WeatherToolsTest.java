package com.nordlyse.springrag.chat;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherToolsTest {

    @Test
    void cityForecastToolsKeepsGeocodingAndTheForecast() {
        var filter = new WeatherTools().cityForecastTools();

        assertThat(filter.test(null, McpSchema.Tool.builder("geocoding").build())).isTrue();
        assertThat(filter.test(null, McpSchema.Tool.builder("weather_forecast").build())).isTrue();
        assertThat(filter.test(null, McpSchema.Tool.builder("marine_weather").build())).isFalse();
    }
}
