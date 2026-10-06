package com.nordlyse.springrag.chat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CityForecastTest {

    @Test
    void cityNameKeepsAnkara() {
        assertThat(PassageQuestion.city("bugun ankara da hava nasil")).isEqualTo("ankara");
    }

    @Test
    void summaryUsesTheOpenMeteoNumbers() {
        String place = """
                {"name":"Ankara","country":"Republic of Türkiye","latitude":39.92,"longitude":32.85}
                """;
        String forecast = """
                {"current":{"temperature_2m":16.7,"weather_code":3,"wind_speed_10m":4.7},
                 "daily":{"temperature_2m_max":[20.8],"temperature_2m_min":[7.2],"precipitation_sum":[0.1]}}
                """;

        String report = CityForecast.summary(JsonMapper.builder().build().readTree(place), forecast);

        assertThat(report).contains("Ankara");
        assertThat(report).contains("16.7");
        assertThat(report).contains("overcast");
        assertThat(report).contains("20.8");
        assertThat(report).contains("7.2");
    }
}
