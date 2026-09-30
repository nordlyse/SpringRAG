package com.nordlyse.springrag.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStorageServiceTest {

    @TempDir
    Path dataDirectory;

    @Test
    void addWritesAllowedFileIntoDataDirectory() throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes());

        StoredDocument stored = service.add(file);

        assertThat(stored.fileName()).endsWith(".txt");
        assertThat(stored.size()).isEqualTo(5);
        assertThat(Files.readString(dataDirectory.resolve(stored.fileName()))).isEqualTo("hello");
    }

    @Test
    void addRejectsDisallowedType() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "bin".getBytes());

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }
}
