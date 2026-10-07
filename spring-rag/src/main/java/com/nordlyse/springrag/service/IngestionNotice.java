package com.nordlyse.springrag.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IngestionNotice {

    private String fileName;
    private String state;
    private String message;
}
