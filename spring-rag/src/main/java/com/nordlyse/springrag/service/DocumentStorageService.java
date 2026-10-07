package com.nordlyse.springrag.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Service
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @Autowired)
public class DocumentStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpeg", "jpg", "txt", "doc", "docx", "xls", "xlsx", "pptx", "ppt", "csv");

    @Value("${spring-rag.data-directory}")
    private Path dataDirectory;

    public Path getDataDirectory() {
        if (dataDirectory == null) {
            return null;
        }
        Path root = dataDirectory.isAbsolute()
                ? dataDirectory
                : Path.of("").toAbsolutePath().resolve(dataDirectory);
        return root.normalize();
    }

    public StoredDocument add(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        String originalName = fileName(file);
        String extension = extension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type is not allowed: " + extension);
        }

        String storedName = safeBase(originalName, extension) + "-" + UUID.randomUUID() + "." + extension;
        Path directory = getDataDirectory();
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("File name is not allowed.");
        }

        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Unable to store " + storedName, exception);
        }

        String mediaType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return new StoredDocument(storedName, file.getSize(), mediaType);
    }

    private static String fileName(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }
        return Path.of(original).getFileName().toString();
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String safeBase(String fileName, String extension) {
        String base = fileName.substring(0, fileName.length() - extension.length() - 1);
        String safe = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "document" : safe;
    }
}
