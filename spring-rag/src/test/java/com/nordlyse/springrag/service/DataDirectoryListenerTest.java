package com.nordlyse.springrag.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataDirectoryListenerTest {

    @TempDir
    Path dataDirectory;

    @Test
    void scanOnceSendsANewFileToIngestion() throws Exception {
        DocumentIngestion ingestion = mock(DocumentIngestion.class);
        Files.writeString(dataDirectory.resolve("notes.txt"), "hello");
        DataDirectoryListener listener = new DataDirectoryListener(dataDirectory, ingestion);

        listener.scanOnce();

        verify(ingestion).ingest(dataDirectory.resolve("notes.txt"));
    }

    @Test
    void scanOnceForgetsARemovedFile() throws Exception {
        DocumentIngestion ingestion = mock(DocumentIngestion.class);
        Path notes = dataDirectory.resolve("notes.txt");
        Files.writeString(notes, "hello");
        DataDirectoryListener listener = new DataDirectoryListener(dataDirectory, ingestion);
        listener.scanOnce();
        Files.delete(notes);

        listener.scanOnce();

        verify(ingestion).forget("notes.txt");
    }
}
