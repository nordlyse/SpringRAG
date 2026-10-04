package com.nordlyse.springrag.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentIngestion {

    static final String FILE_NAME = "file_name";

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestion.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpeg", "jpg", "txt", "doc", "docx", "xls", "xlsx", "pptx", "ppt", "csv");

    private final Path dataDirectory;
    private final VectorStore vectorStore;
    private final IngestionBoard board;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(160)
            .withMinChunkSizeChars(80)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(200)
            .withKeepSeparator(true)
            .build();
    private final ConcurrentHashMap<String, Fingerprint> indexed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    DocumentIngestion(
            @Value("${spring-rag.data-directory}") Path dataDirectory,
            VectorStore vectorStore,
            IngestionBoard board) {
        Path root = dataDirectory.isAbsolute()
                ? dataDirectory
                : Path.of("").toAbsolutePath().resolve(dataDirectory);
        this.dataDirectory = root.normalize();
        this.vectorStore = vectorStore;
        this.board = board;
    }

    public void ingestStored(String fileName) {
        Path path = dataDirectory.resolve(fileName).normalize();
        if (!path.startsWith(dataDirectory)) {
            board.failed(fileName, fileName + " could not be scanned.");
            return;
        }
        ingest(path);
    }

    public void ingest(Path path) {
        if (!Files.isRegularFile(path) || !allowed(path)) {
            return;
        }
        String fileName = path.getFileName().toString();
        if (fileName.startsWith(".")) {
            return;
        }
        synchronized (locks.computeIfAbsent(fileName, ignored -> new Object())) {
            try {
                Fingerprint fingerprint = fingerprint(path);
                if (fingerprint.equals(indexed.get(fileName))) {
                    return;
                }
                board.scanning(fileName);
                String text = DocumentText.read(path);
                if (text == null || text.isBlank()) {
                    board.failed(fileName, fileName + " has no readable text.");
                    return;
                }
                replacePassages(fileName, text);
                indexed.put(fileName, fingerprint);
                board.ready(fileName);
            }
            catch (RuntimeException | IOException exception) {
                log.warn("Unable to scan {}", fileName, exception);
                board.failed(fileName, fileName + " could not be scanned.");
            }
        }
    }

    public void forget(String fileName) {
        synchronized (locks.computeIfAbsent(fileName, ignored -> new Object())) {
            indexed.remove(fileName);
            board.remove(fileName);
            deletePassages(fileName);
        }
    }

    private void replacePassages(String fileName, String text) {
        deletePassages(fileName);
        List<Document> chunks = splitter.split(Document.builder().text(text).build()).stream()
                .filter(chunk -> chunk.getText() != null && !chunk.getText().isBlank())
                .map(chunk -> Document.builder()
                        .text(chunk.getText())
                        .metadata(FILE_NAME, fileName)
                        .build())
                .toList();
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
    }

    private void deletePassages(String fileName) {
        try {
            vectorStore.delete(new FilterExpressionBuilder().eq(FILE_NAME, fileName).build());
        }
        catch (RuntimeException exception) {
            log.warn("Unable to replace passages for {}", fileName, exception);
        }
    }

    private static Fingerprint fingerprint(Path path) throws IOException {
        return new Fingerprint(Files.size(path), Files.getLastModifiedTime(path).toMillis());
    }

    private static boolean allowed(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private record Fingerprint(long size, long modified) {
    }
}
