package com.nordlyse.springrag.controller;

import java.util.List;

import com.nordlyse.springrag.service.DocumentIngestion;
import com.nordlyse.springrag.service.DocumentStorageService;
import com.nordlyse.springrag.service.IngestionBoard;
import com.nordlyse.springrag.service.IngestionNotice;
import com.nordlyse.springrag.service.StoredDocument;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(DocumentExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentStorageService documentStorageService;

    @MockitoBean
    private DocumentIngestion documentIngestion;

    @MockitoBean
    private IngestionBoard ingestionBoard;

    @Test
    void addAcceptsMultipartFile() throws Exception {
        when(documentStorageService.add(any())).thenReturn(new StoredDocument("notes.txt", 5, "text/plain"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/documents").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].fileName").value("notes.txt"))
                .andExpect(jsonPath("$[0].size").value(5));
        verify(documentIngestion).ingestStored("notes.txt");
    }

    @Test
    void noticesReturnsScanMessages() throws Exception {
        when(ingestionBoard.notices()).thenReturn(List.of(
                new IngestionNotice("cv.pdf", "ready", "cv.pdf was scanned and is ready to use.")));

        mockMvc.perform(get("/documents/ingestion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("ready"))
                .andExpect(jsonPath("$[0].message").value("cv.pdf was scanned and is ready to use."));
    }

    @Test
    void addRejectsDisallowedType() throws Exception {
        when(documentStorageService.add(any())).thenThrow(new IllegalArgumentException("File type is not allowed: exe"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "bin".getBytes());

        mockMvc.perform(multipart("/documents").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addStoresSeveralFiles() throws Exception {
        when(documentStorageService.add(any()))
                .thenReturn(new StoredDocument("a.txt", 1, "text/plain"))
                .thenReturn(new StoredDocument("b.pdf", 2, "application/pdf"));
        MockMultipartFile first = new MockMultipartFile("file", "a.txt", "text/plain", "a".getBytes());
        MockMultipartFile second = new MockMultipartFile("file", "b.pdf", "application/pdf", "bb".getBytes());

        mockMvc.perform(multipart("/documents").file(first).file(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].fileName").value("b.pdf"));
    }

    @Test
    void addRejectsARequestWithoutAFile() throws Exception {
        mockMvc.perform(multipart("/documents"))
                .andExpect(status().isBadRequest());
    }
}
