package com.nordlyse.springrag.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.nordlyse.springrag.service.StoredDocument;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDocumentRecordStore implements DocumentRecordStore {

    private static final String TABLE = """
            CREATE TABLE IF NOT EXISTS documents (
                file_name varchar(512) PRIMARY KEY,
                media_type varchar(255) NOT NULL,
                size_bytes bigint NOT NULL
            )
            """;

    private static final RowMapper<StoredDocument> ROW = JdbcDocumentRecordStore::mapRow;

    private final JdbcTemplate jdbcTemplate;

    private volatile boolean tableReady;

    public JdbcDocumentRecordStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void add(StoredDocument document) {
        ensureTable();
        jdbcTemplate.update(
                "INSERT INTO documents (file_name, media_type, size_bytes) VALUES (?, ?, ?)",
                document.getFileName(),
                document.getMediaType(),
                document.getSize());
    }

    @Override
    public void update(StoredDocument document) {
        ensureTable();
        jdbcTemplate.update(
                "UPDATE documents SET media_type = ?, size_bytes = ? WHERE file_name = ?",
                document.getMediaType(),
                document.getSize(),
                document.getFileName());
    }

    @Override
    public int remove(String fileName) {
        ensureTable();
        return jdbcTemplate.update("DELETE FROM documents WHERE file_name = ?", fileName);
    }

    @Override
    public Optional<StoredDocument> find(String fileName) {
        ensureTable();
        List<StoredDocument> rows = jdbcTemplate.query(
                "SELECT file_name, media_type, size_bytes FROM documents WHERE file_name = ?",
                ROW,
                fileName);
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public List<StoredDocument> list() {
        ensureTable();
        List<StoredDocument> rows = jdbcTemplate.query(
                "SELECT file_name, media_type, size_bytes FROM documents ORDER BY file_name",
                ROW);
        return rows == null ? List.of() : List.copyOf(rows);
    }

    private void ensureTable() {
        if (tableReady) {
            return;
        }
        jdbcTemplate.execute(TABLE);
        tableReady = true;
    }

    private static StoredDocument mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StoredDocument(
                resultSet.getString("file_name"),
                resultSet.getLong("size_bytes"),
                resultSet.getString("media_type"));
    }
}
