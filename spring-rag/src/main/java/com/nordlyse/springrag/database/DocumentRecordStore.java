package com.nordlyse.springrag.database;

import java.util.List;
import java.util.Optional;

import com.nordlyse.springrag.service.StoredDocument;

public interface DocumentRecordStore {

    void add(StoredDocument document);

    void update(StoredDocument document);

    int remove(String fileName);

    Optional<StoredDocument> find(String fileName);

    List<StoredDocument> list();
}
