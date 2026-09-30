package com.nordlyse.springrag.database;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VectorDatabaseSettings.class)
public class VectorDatabaseConfig {

    @Bean
    DataSource vectorDatabase(VectorDatabaseSettings settings) {
        if (settings.url() == null || settings.url().isBlank()) {
            throw new IllegalStateException("Vector database URL is required.");
        }
        DataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(settings.url())
                .username(settings.username())
                .password(settings.password())
                .build();
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.setPoolName("vector-database");
            hikari.setInitializationFailTimeout(-1);
        }
        return dataSource;
    }
}
