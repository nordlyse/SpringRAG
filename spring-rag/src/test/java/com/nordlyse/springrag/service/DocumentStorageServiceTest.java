package com.nordlyse.springrag.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "pdf", "png", "jpeg", "jpg", "txt", "doc", "docx", "xls", "xlsx", "pptx", "ppt", "csv"
    })
    void addAcceptsDocumentType(String extension) throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report." + extension, "application/octet-stream", "body".getBytes());

        StoredDocument stored = service.add(file);

        assertThat(stored.fileName()).endsWith("." + extension);
        assertThat(Files.readString(dataDirectory.resolve(stored.fileName()))).isEqualTo("body");
    }

    @Test
    void addStoresOnlyTheBaseName() throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "../secret.txt", "text/plain", "hidden".getBytes());

        StoredDocument stored = service.add(file);
        Path storedPath = dataDirectory.resolve(stored.fileName());

        assertThat(stored.fileName()).startsWith("secret-");
        assertThat(storedPath.getParent()).isEqualTo(dataDirectory);
        assertThat(Files.readString(storedPath)).isEqualTo("hidden");
    }

    @Test
    void addNormalizesTheExtension() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "NOTES.PDF", "application/pdf", "page".getBytes());

        StoredDocument stored = service.add(file);

        assertThat(stored.fileName()).startsWith("NOTES-").endsWith(".pdf");
    }

    @Test
    void addReplacesUnsafeCharacters() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", "@@@.txt", "text/plain", "x".getBytes());

        assertThat(service.add(file).fileName()).startsWith("___-");
    }

    @Test
    void addUsesDocumentWhenTheBaseNameIsEmpty() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file", ".txt", "text/plain", "x".getBytes());

        assertThat(service.add(file).fileName()).startsWith("document-");
    }

    @Test
    void addUsesAGenericMediaTypeWhenNoneIsGiven() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", null, "x".getBytes());

        assertThat(service.add(file).mediaType()).isEqualTo("application/octet-stream");
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

    @Test
    void addRejectsANullFile() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);

        assertThatThrownBy(() -> service.add(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void addRejectsABlankName() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", "   ", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void addRejectsANameWithoutAnExtension() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", "notes", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void addRejectsATrailingDot() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MockMultipartFile file = new MockMultipartFile("file", "notes.", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void addWrapsAReadFailure() throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("notes.txt");
        when(file.getInputStream()).thenThrow(new IOException("unreadable"));

        assertThatThrownBy(() -> service.add(file))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Unable to store");
    }

    @Test
    void replaceOverwritesTheStoredFile() throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        StoredDocument stored = service.add(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()));
        MockMultipartFile updated = new MockMultipartFile("file", "notes.txt", "text/plain", "updated".getBytes());

        StoredDocument replaced = service.replace(stored.fileName(), updated);

        assertThat(replaced.fileName()).isEqualTo(stored.fileName());
        assertThat(Files.readString(dataDirectory.resolve(stored.fileName()))).isEqualTo("updated");
    }

    @Test
    void removeDeletesTheStoredFile() throws Exception {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);
        StoredDocument stored = service.add(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()));

        service.remove(stored.fileName());

        assertThat(Files.exists(dataDirectory.resolve(stored.fileName()))).isFalse();
    }

    @Test
    void removeRejectsAPathOutsideTheDataDirectory() {
        DocumentStorageService service = new DocumentStorageService(dataDirectory);

        assertThatThrownBy(() -> service.remove("../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }
}
