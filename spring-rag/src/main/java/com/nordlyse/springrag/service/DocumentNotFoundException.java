package com.nordlyse.springrag.service;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String fileName) {
        super("Document was not found: " + fileName);
    }
}
