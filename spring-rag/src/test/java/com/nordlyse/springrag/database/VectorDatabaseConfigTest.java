package com.nordlyse.springrag.database;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorDatabaseConfigTest {

    @Test
    void vectorDatabaseUsesTheConfiguredConnection() {
        VectorDatabaseSettings settings = new VectorDatabaseSettings(
                "jdbc:postgresql://pgvector:5432/springrag",
                "springrag",
                "springrag",
                "public",
                "vector_store");

        try (HikariDataSource dataSource = (HikariDataSource) new VectorDatabaseConfig().vectorDatabase(settings)) {
            assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://pgvector:5432/springrag");
            assertThat(dataSource.getUsername()).isEqualTo("springrag");
            assertThat(dataSource.getPassword()).isEqualTo("springrag");
            assertThat(dataSource.getDriverClassName()).isEqualTo("org.postgresql.Driver");
            assertThat(dataSource.getPoolName()).isEqualTo("vector-database");
            assertThat(dataSource.getInitializationFailTimeout()).isEqualTo(-1);
        }
    }

    @Test
    void vectorDatabaseRejectsABlankUrl() {
        VectorDatabaseSettings settings = new VectorDatabaseSettings(" ", "springrag", "springrag", "public", "vector_store");

        assertThatThrownBy(() -> new VectorDatabaseConfig().vectorDatabase(settings))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("URL");
    }
}
