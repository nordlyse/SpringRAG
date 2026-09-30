package com.nordlyse.springrag.database;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring-rag.database")
public record VectorDatabaseSettings(String url, String username, String password, String schema, String table) {
}
