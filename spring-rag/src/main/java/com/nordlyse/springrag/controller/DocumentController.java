package com.nordlyse.springrag.controller;

import java.util.List;

import com.nordlyse.springrag.service.DocumentIngestion;
import com.nordlyse.springrag.service.DocumentStorageService;
import com.nordlyse.springrag.service.IngestionBoard;
import com.nordlyse.springrag.service.IngestionNotice;
import com.nordlyse.springrag.service.StoredDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RestController
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @Autowired)
public class DocumentController {

    private DocumentStorageService documentStorageService;
    private DocumentIngestion documentIngestion;
    private IngestionBoard ingestionBoard;

    @PostMapping(path = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<StoredDocument>> add(@RequestParam("file") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required.");
        }
        List<StoredDocument> stored = files.stream().map(documentStorageService::add).toList();
        stored.forEach(document -> documentIngestion.ingestStored(document.getFileName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    @GetMapping("/documents/ingestion")
    public List<IngestionNotice> notices() {
        return ingestionBoard.notices();
    }
}
