package com.nordlyse.springrag.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoredDocument {

    private String fileName;
    private long size;
    private String mediaType;
}
