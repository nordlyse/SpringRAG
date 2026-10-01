package com.nordlyse.springrag.database;

import com.nordlyse.springrag.service.StoredDocument;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcDocumentRecordStoreTest {

    @Test
    void removeDeletesTheDatabaseRow() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), eq("notes.txt"))).thenReturn(1);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        int removed = store.remove("notes.txt");

        assertThat(removed).isEqualTo(1);
        verify(jdbcTemplate).execute(anyString());
        verify(jdbcTemplate).update("DELETE FROM documents WHERE file_name = ?", "notes.txt");
    }

    @Test
    void addInsertsTheDocumentRow() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        store.add(new StoredDocument("notes.txt", 5, "text/plain"));

        verify(jdbcTemplate).update(
                "INSERT INTO documents (file_name, media_type, size_bytes) VALUES (?, ?, ?)",
                "notes.txt",
                "text/plain",
                5L);
    }
}
