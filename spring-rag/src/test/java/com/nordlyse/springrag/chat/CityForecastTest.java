package com.nordlyse.springrag.chat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CityForecastTest {

    @Test
    void cityNameKeepsAnkara() {
        assertThat(PassageQuestion.city("bugun ankara da hava nasil")).isEqualTo("ankara");
    }

    @Test
    void summaryNamesTheAskedDay() throws Exception {
        var json = JsonMapper.builder().build();
        var place = json.readTree("""
                {"name":"Ankara","country":"Republic of Türkiye","latitude":39.92,"longitude":32.85}
                """);
        String today = """
                {"current":{"temperature_2m":16.7,"weather_code":3,"wind_speed_10m":4.7},
                 "daily":{"temperature_2m_max":[20.8],"temperature_2m_min":[7.2],"precipitation_sum":[0.1],"weather_code":[3]}}
                """;
        String yesterday = """
                {"daily":{"time":["2026-10-05"],"temperature_2m_max":[20.6],"temperature_2m_min":[5.9],"precipitation_sum":[0.0],"weather_code":[2]}}
                """;

        String todayReport = CityForecast.summary(place, today, LocalDate.of(2026, 10, 6), "today", true);
        String yesterdayReport = CityForecast.summary(
                place, yesterday, LocalDate.of(2026, 10, 5), "yesterday", false);

        assertThat(todayReport).contains("16.7", "overcast", "20.8", "7.2", "2026-10-06", "today");
        assertThat(yesterdayReport).contains("2026-10-05", "yesterday", "partly cloudy", "20.6", "5.9");
        assertThat(yesterdayReport).doesNotContain("Now:").doesNotContain("today");
    }

    @Test
    void spokenTokatAnswerUsesOnlyTheReport() throws Exception {
        var place = JsonMapper.builder().build().readTree("""
                {"name":"Tokat Province","country":"Republic of Türkiye","latitude":40.31,"longitude":36.55}
                """);
        String forecast = """
                {"daily":{"time":["2026-10-03"],"temperature_2m_max":[15.1],"temperature_2m_min":[7.6],"precipitation_sum":[0.4],"weather_code":[80]}}
                """;

        String answer = CityForecast.spoken(place, forecast, LocalDate.of(2026, 10, 3), false, true);

        assertThat(answer).isEqualTo(
                "3 Ekim 2026 tarihinde Tokat için hava yağmurluydu. En yüksek sıcaklık 15.1°C, en düşük 7.6°C, yağış 0.4 mm. Kaynak Open-Meteo.");
    }

    @Test
    void yesterdayAsksForThatDateOnly() {
        Map<String, Object> args = CityForecast.arguments(39.92, 32.85, LocalDate.of(2026, 10, 5), false);

        assertThat(args).containsEntry("start_date", "2026-10-05").containsEntry("end_date", "2026-10-05");
        assertThat(args).doesNotContainKey("forecast_days").doesNotContainKey("current");
        assertThat(CityForecast.toolFor(WeatherWhen.Kind.FORECAST)).isEqualTo("weather_forecast");
        assertThat(CityForecast.toolFor(WeatherWhen.Kind.ARCHIVE)).isEqualTo("weather_archive");
    }
}
