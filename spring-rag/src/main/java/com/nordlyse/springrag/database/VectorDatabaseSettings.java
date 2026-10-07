package com.nordlyse.springrag.database;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ConfigurationProperties(prefix = "spring-rag.database")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VectorDatabaseSettings {

    private String url;
    private String username;
    private String password;
    private String schema;
    private String table;
}
