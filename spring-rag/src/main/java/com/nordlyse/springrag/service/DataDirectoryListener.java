package com.nordlyse.springrag.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(name = "spring-rag.directory-listener", havingValue = "true", matchIfMissing = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class DataDirectoryListener implements SmartLifecycle {

    private Path dataDirectory;
    private DocumentIngestion ingestion;
    private Set<String> seen = new HashSet<>();
    private volatile boolean running;
    private Thread worker;

    @Autowired
    DataDirectoryListener(
            @Value("${spring-rag.data-directory}") Path dataDirectory,
            DocumentIngestion ingestion) {
        Path root = dataDirectory.isAbsolute()
                ? dataDirectory
                : Path.of("").toAbsolutePath().resolve(dataDirectory);
        this.dataDirectory = root.normalize();
        this.ingestion = ingestion;
    }

    @Override
    public void start() {
        running = true;
        worker = Thread.ofPlatform().name("data-directory-listener").daemon(true).unstarted(this::loop);
        worker.start();
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    void scanOnce() throws IOException {
        Files.createDirectories(dataDirectory);
        Set<String> present = new HashSet<>();
        try (Stream<Path> files = Files.list(dataDirectory)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith(".")) {
                    return;
                }
                present.add(fileName);
                ingestion.ingest(path);
            });
        }
        for (String fileName : Set.copyOf(seen)) {
            if (!present.contains(fileName)) {
                ingestion.forget(fileName);
            }
        }
        seen.clear();
        seen.addAll(present);
    }

    private void loop() {
        try (WatchService watchService = dataDirectory.getFileSystem().newWatchService()) {
            Files.createDirectories(dataDirectory);
            dataDirectory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            while (running) {
                scanOnce();
                WatchKey key = watchService.poll(2, TimeUnit.SECONDS);
                if (key != null) {
                    key.pollEvents();
                    key.reset();
                }
            }
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        catch (IOException exception) {
            log.warn("Data directory listener stopped: {}", exception.getMessage());
        }
        finally {
            running = false;
        }
    }
}
