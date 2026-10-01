package com.nordlyse.springrag.database;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import com.nordlyse.springrag.service.StoredDocument;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Test
    void addPreparesTheTableOnlyOnce() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        store.add(new StoredDocument("a.txt", 1, "text/plain"));
        store.add(new StoredDocument("b.txt", 2, "text/plain"));

        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void updateChangesTheDocumentRow() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        store.update(new StoredDocument("notes.txt", 9, "text/plain"));

        verify(jdbcTemplate).update(
                "UPDATE documents SET media_type = ?, size_bytes = ? WHERE file_name = ?",
                "text/plain",
                9L,
                "notes.txt");
    }

    @Test
    void findReturnsTheMatchingDocument() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), anyRowMapper(), eq("notes.txt"))).thenAnswer(JdbcDocumentRecordStoreTest::readRow);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        Optional<StoredDocument> found = store.find("notes.txt");

        assertThat(found).contains(new StoredDocument("notes.txt", 5, "text/plain"));
    }

    @Test
    void findReturnsEmptyWhenNoRowMatches() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), anyRowMapper(), eq("missing.txt"))).thenReturn(List.of());
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        assertThat(store.find("missing.txt")).isEmpty();
    }

    @Test
    void findReturnsEmptyWhenTheQueryResultIsNull() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), anyRowMapper(), eq("missing.txt"))).thenReturn(null);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        assertThat(store.find("missing.txt")).isEmpty();
    }

    @Test
    void listReturnsDocumentsInStoredOrder() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), anyRowMapper())).thenAnswer(JdbcDocumentRecordStoreTest::readRow);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        assertThat(store.list()).containsExactly(new StoredDocument("notes.txt", 5, "text/plain"));
    }

    @Test
    void listReturnsEmptyWhenTheQueryResultIsNull() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), anyRowMapper())).thenReturn(null);
        JdbcDocumentRecordStore store = new JdbcDocumentRecordStore(jdbcTemplate);

        assertThat(store.list()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static RowMapper<StoredDocument> anyRowMapper() {
        return any(RowMapper.class);
    }

    @SuppressWarnings("unchecked")
    private static List<StoredDocument> readRow(InvocationOnMock invocation) throws Exception {
        RowMapper<StoredDocument> mapper = invocation.getArgument(1);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("file_name")).thenReturn("notes.txt");
        when(resultSet.getLong("size_bytes")).thenReturn(5L);
        when(resultSet.getString("media_type")).thenReturn("text/plain");
        return List.of(mapper.mapRow(resultSet, 1));
    }
}
