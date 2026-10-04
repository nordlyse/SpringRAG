package com.nordlyse.springrag.controller;

import java.util.List;

import com.nordlyse.springrag.service.DocumentIngestion;
import com.nordlyse.springrag.service.DocumentNotFoundException;
import com.nordlyse.springrag.service.DocumentService;
import com.nordlyse.springrag.service.IngestionBoard;
import com.nordlyse.springrag.service.IngestionNotice;
import com.nordlyse.springrag.service.StoredDocument;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentIngestion documentIngestion;
    private final IngestionBoard ingestionBoard;

    public DocumentController(
            DocumentService documentService,
            DocumentIngestion documentIngestion,
            IngestionBoard ingestionBoard) {
        this.documentService = documentService;
        this.documentIngestion = documentIngestion;
        this.ingestionBoard = ingestionBoard;
    }

    @GetMapping("/documents")
    public List<StoredDocument> list() {
        return documentService.list();
    }

    @GetMapping("/documents/ingestion")
    public List<IngestionNotice> notices() {
        return ingestionBoard.notices();
    }

    @GetMapping("/documents/{fileName:.+}")
    public StoredDocument find(@PathVariable String fileName) {
        return documentService.find(fileName)
                .orElseThrow(() -> new DocumentNotFoundException(fileName));
    }

    @PostMapping(path = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<StoredDocument>> add(@RequestParam("file") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required.");
        }
        List<StoredDocument> stored = files.stream().map(documentService::add).toList();
        stored.forEach(document -> documentIngestion.ingestStored(document.fileName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    @PutMapping(path = "/documents/{fileName:.+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredDocument replace(@PathVariable String fileName, @RequestParam("file") MultipartFile file) {
        StoredDocument stored = documentService.replace(fileName, file);
        documentIngestion.ingestStored(stored.fileName());
        return stored;
    }

    @DeleteMapping("/documents/{fileName:.+}")
    public ResponseEntity<Void> remove(@PathVariable String fileName) {
        documentService.remove(fileName);
        documentIngestion.forget(fileName);
        return ResponseEntity.noContent().build();
    }
}
