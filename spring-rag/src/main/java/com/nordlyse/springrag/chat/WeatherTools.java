package com.nordlyse.springrag.chat;

import java.util.Arrays;
import java.util.Set;

import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@Getter
@Setter
@NoArgsConstructor
public class WeatherTools {

    static final Set<String> CITY_FORECAST = Set.of("geocoding", "weather_forecast");

    @Bean
    McpToolFilter cityForecastTools() {
        return (connection, tool) -> CITY_FORECAST.contains(tool.name());
    }

    static ToolCallbackProvider repaired(ToolCallbackProvider tools) {
        return () -> Arrays.stream(tools.getToolCallbacks())
                .map(callback -> new RepairedWeatherCallback(callback))
                .toArray(ToolCallback[]::new);
    }
}
