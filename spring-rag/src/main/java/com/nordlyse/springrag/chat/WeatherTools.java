package com.nordlyse.springrag.chat;

import java.util.Set;

import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherTools {

    static final Set<String> CITY_FORECAST = Set.of("geocoding", "weather_forecast");

    @Bean
    McpToolFilter cityForecastTools() {
        return (connection, tool) -> CITY_FORECAST.contains(tool.name());
    }
}
