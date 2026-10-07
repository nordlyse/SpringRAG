package com.nordlyse.springrag.database;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@EnableConfigurationProperties(VectorDatabaseSettings.class)
@Getter
@Setter
@NoArgsConstructor
public class VectorDatabaseConfig {

    @Bean
    DataSource vectorDatabase(VectorDatabaseSettings settings) {
        if (settings.getUrl() == null || settings.getUrl().isBlank()) {
            throw new IllegalStateException("Vector database URL is required.");
        }
        DataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(settings.getUrl())
                .username(settings.getUsername())
                .password(settings.getPassword())
                .build();
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.setPoolName("vector-database");
            hikari.setInitializationFailTimeout(-1);
        }
        return dataSource;
    }

    @Bean
    JdbcTemplate applicationJdbc(DataSource vectorDatabase) {
        return new JdbcTemplate(vectorDatabase);
    }
}
