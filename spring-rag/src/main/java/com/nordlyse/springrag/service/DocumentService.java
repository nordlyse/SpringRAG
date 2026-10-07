package com.nordlyse.springrag.service;

import java.util.List;
import java.util.Optional;

import com.nordlyse.springrag.database.DocumentRecordStore;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentStorageService documentStorageService;

    private final DocumentRecordStore documentRecordStore;

    public DocumentService(DocumentStorageService documentStorageService, DocumentRecordStore documentRecordStore) {
        this.documentStorageService = documentStorageService;
        this.documentRecordStore = documentRecordStore;
    }

    public StoredDocument add(MultipartFile file) {
        StoredDocument stored = documentStorageService.add(file);
        try {
            documentRecordStore.add(stored);
        }
        catch (RuntimeException exception) {
            documentStorageService.remove(stored.getFileName());
            throw exception;
        }
        return stored;
    }

    public List<StoredDocument> list() {
        return documentRecordStore.list();
    }

    public Optional<StoredDocument> find(String fileName) {
        return documentRecordStore.find(fileName);
    }

    public StoredDocument replace(String fileName, MultipartFile file) {
        if (documentRecordStore.find(fileName).isEmpty()) {
            throw new DocumentNotFoundException(fileName);
        }
        StoredDocument stored = documentStorageService.replace(fileName, file);
        documentRecordStore.update(stored);
        return stored;
    }

    public void remove(String fileName) {
        if (documentRecordStore.remove(fileName) == 0) {
            throw new DocumentNotFoundException(fileName);
        }
        documentStorageService.remove(fileName);
    }
}
