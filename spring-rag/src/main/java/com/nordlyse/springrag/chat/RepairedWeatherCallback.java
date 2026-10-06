package com.nordlyse.springrag.chat;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

final class RepairedWeatherCallback implements ToolCallback {

    private final ToolCallback delegate;

    RepairedWeatherCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(WeatherArgumentRepair.repair(name(), toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return delegate.call(WeatherArgumentRepair.repair(name(), toolInput), toolContext);
    }

    private String name() {
        return delegate.getToolDefinition().name();
    }
}
