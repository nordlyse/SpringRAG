package com.nordlyse.springrag.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DocumentIngestionTest {

    @TempDir
    Path dataDirectory;

    @Test
    void ingestSplitsTextAndWritesPassages() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        IngestionBoard board = new IngestionBoard();
        DocumentIngestion ingestion = new DocumentIngestion(dataDirectory, vectorStore, board);
        Files.writeString(dataDirectory.resolve("notes.txt"), "Worked at Nordlyse in Oslo.");

        ingestion.ingestStored("notes.txt");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> passages = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(passages.capture());
        assertThat(passages.getValue()).isNotEmpty();
        assertThat(passages.getValue().get(0).getText()).contains("Nordlyse in Oslo");
        assertThat(passages.getValue().get(0).getMetadata()).containsEntry(DocumentIngestion.FILE_NAME, "notes.txt");
        assertThat(board.notices()).anySatisfy(notice -> {
            assertThat(notice.state()).isEqualTo("ready");
            assertThat(notice.message()).isEqualTo("notes.txt was scanned and is ready to use.");
        });
    }

    @Test
    void ingestSkipsAFileThatWasAlreadyScanned() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        DocumentIngestion ingestion = new DocumentIngestion(dataDirectory, vectorStore, new IngestionBoard());
        Files.writeString(dataDirectory.resolve("notes.txt"), "Worked at Nordlyse in Oslo.");

        ingestion.ingestStored("notes.txt");
        ingestion.ingestStored("notes.txt");

        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void ingestReportsAFileWithNoReadableText() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        IngestionBoard board = new IngestionBoard();
        DocumentIngestion ingestion = new DocumentIngestion(dataDirectory, vectorStore, board);
        Files.writeString(dataDirectory.resolve("empty.txt"), "   ");

        ingestion.ingestStored("empty.txt");

        verify(vectorStore, times(0)).add(anyList());
        assertThat(board.notices()).anySatisfy(notice -> {
            assertThat(notice.state()).isEqualTo("failed");
            assertThat(notice.message()).isEqualTo("empty.txt has no readable text.");
        });
    }
}
