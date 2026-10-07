package com.nordlyse.springrag.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nordlyse.springrag.database.DocumentRecordStore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentServiceTest {

    @TempDir
    Path dataDirectory;

    @Test
    void addListsAndReplacesADocument() throws Exception {
        MemoryDocumentRecordStore records = new MemoryDocumentRecordStore();
        DocumentService service = new DocumentService(new DocumentStorageService(dataDirectory), records);
        MockMultipartFile original = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        StoredDocument stored = service.add(original);

        assertThat(service.list()).containsExactly(stored);
        assertThat(service.find(stored.getFileName())).contains(stored);
        assertThat(Files.readString(dataDirectory.resolve(stored.getFileName()))).isEqualTo("hello");

        MockMultipartFile updated = new MockMultipartFile("file", "notes.txt", "text/plain", "updated".getBytes());
        StoredDocument replaced = service.replace(stored.getFileName(), updated);

        assertThat(replaced.getFileName()).isEqualTo(stored.getFileName());
        assertThat(replaced.getSize()).isEqualTo(7);
        assertThat(Files.readString(dataDirectory.resolve(stored.getFileName()))).isEqualTo("updated");
        assertThat(service.find(stored.getFileName())).contains(replaced);
    }

    @Test
    void removeDeletesTheFileAndTheDatabaseRecord() throws Exception {
        MemoryDocumentRecordStore records = new MemoryDocumentRecordStore();
        DocumentService service = new DocumentService(new DocumentStorageService(dataDirectory), records);
        StoredDocument stored = service.add(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()));

        service.remove(stored.getFileName());

        assertThat(service.list()).isEmpty();
        assertThat(service.find(stored.getFileName())).isEmpty();
        assertThat(Files.exists(dataDirectory.resolve(stored.getFileName()))).isFalse();
    }

    @Test
    void replaceRejectsAMissingDocument() {
        DocumentService service = new DocumentService(new DocumentStorageService(dataDirectory), new MemoryDocumentRecordStore());
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.replace("notes.txt", file))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void removeRejectsAMissingDocument() {
        DocumentService service = new DocumentService(new DocumentStorageService(dataDirectory), new MemoryDocumentRecordStore());

        assertThatThrownBy(() -> service.remove("notes.txt"))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    private static final class MemoryDocumentRecordStore implements DocumentRecordStore {

        private final Map<String, StoredDocument> rows = new LinkedHashMap<>();

        @Override
        public void add(StoredDocument document) {
            rows.put(document.getFileName(), document);
        }

        @Override
        public void update(StoredDocument document) {
            rows.put(document.getFileName(), document);
        }

        @Override
        public int remove(String fileName) {
            return rows.remove(fileName) == null ? 0 : 1;
        }

        @Override
        public Optional<StoredDocument> find(String fileName) {
            return Optional.ofNullable(rows.get(fileName));
        }

        @Override
        public List<StoredDocument> list() {
            return List.copyOf(rows.values());
        }
    }
}
