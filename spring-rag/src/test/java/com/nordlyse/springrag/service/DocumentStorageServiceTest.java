package com.nordlyse.springrag.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

        assertThat(stored.getFileName()).endsWith(".txt");
        assertThat(stored.getSize()).isEqualTo(5);
        assertThat(Files.readString(dataDirectory.resolve(stored.getFileName()))).isEqualTo("hello");
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

    @ParameterizedTest
    @ValueSource(strings = {
            "pdf", "png", "jpeg", "jpg", "txt", "doc", "docx", "xls", "xlsx", "pptx", "ppt", "csv"
    })
    void addAcceptsDocumentType(String extension) throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report." + extension, "application/octet-stream", "body".getBytes());

        StoredDocument stored = service.add(file);

        assertThat(stored.getFileName()).endsWith("." + extension);
        assertThat(Files.readString(dataDirectory.resolve(stored.getFileName()))).isEqualTo("body");
    }

    @Test
    void addStoresOnlyTheBaseName() throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "../secret.txt", "text/plain", "hidden".getBytes());

        StoredDocument stored = service.add(file);
        Path storedPath = dataDirectory.resolve(stored.getFileName());

        assertThat(stored.getFileName()).startsWith("secret-");
        assertThat(storedPath.getParent()).isEqualTo(dataDirectory);
        assertThat(Files.readString(storedPath)).isEqualTo("hidden");
    }

    @Test
    void addNormalizesTheExtension() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "NOTES.PDF", "application/pdf", "page".getBytes());

        StoredDocument stored = service.add(file);

        assertThat(stored.getFileName()).startsWith("NOTES-").endsWith(".pdf");
    }

    @Test
    void addReplacesUnsafeCharacters() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "@@@.txt", "text/plain", "x".getBytes());

        assertThat(service.add(file).getFileName()).startsWith("___-");
    }

    @Test
    void addUsesDocumentWhenTheBaseNameIsEmpty() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", ".txt", "text/plain", "x".getBytes());

        assertThat(service.add(file).getFileName()).startsWith("document-");
    }

    @Test
    void addUsesAGenericMediaTypeWhenNoneIsGiven() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", null, "x".getBytes());

        assertThat(service.add(file).getMediaType()).isEqualTo("application/octet-stream");
    }

    @Test
    void addRejectsAnEmptyFile() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void addRejectsAMissingName() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", null, "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }
}
