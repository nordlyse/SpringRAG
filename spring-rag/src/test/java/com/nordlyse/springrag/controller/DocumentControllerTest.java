package com.nordlyse.springrag.controller;

import java.util.List;
import java.util.Optional;

import com.nordlyse.springrag.service.DocumentNotFoundException;
import com.nordlyse.springrag.service.DocumentService;
import com.nordlyse.springrag.service.StoredDocument;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(DocumentExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentController documentController;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void listReturnsStoredDocuments() throws Exception {
        when(documentService.list()).thenReturn(List.of(new StoredDocument("notes.txt", 5, "text/plain")));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("notes.txt"))
                .andExpect(jsonPath("$[0].size").value(5));
    }

    @Test
    void findReturnsOneDocument() throws Exception {
        when(documentService.find("notes.txt")).thenReturn(Optional.of(new StoredDocument("notes.txt", 5, "text/plain")));

        mockMvc.perform(get("/documents/notes.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("notes.txt"));
    }

    @Test
    void findRejectsAMissingDocument() throws Exception {
        when(documentService.find("missing.txt")).thenReturn(Optional.empty());

        mockMvc.perform(get("/documents/missing.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAcceptsMultipartFile() throws Exception {
        when(documentService.add(any())).thenReturn(new StoredDocument("notes.txt", 5, "text/plain"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/documents").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].fileName").value("notes.txt"))
                .andExpect(jsonPath("$[0].size").value(5));
    }

    @Test
    void addRejectsDisallowedType() throws Exception {
        when(documentService.add(any())).thenThrow(new IllegalArgumentException("File type is not allowed: exe"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "bin".getBytes());

        mockMvc.perform(multipart("/documents").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addStoresSeveralFiles() throws Exception {
        when(documentService.add(any()))
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

    @Test
    void addRejectsAnEmptyFileList() {
        assertThatThrownBy(() -> documentController.add(List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("required");
    }

    @Test
    void addRejectsANullFileList() {
        assertThatThrownBy(() -> documentController.add(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("required");
    }

    @Test
    void replaceUpdatesTheDocument() throws Exception {
        when(documentService.replace(eq("notes.txt"), any())).thenReturn(new StoredDocument("notes.txt", 9, "text/plain"));
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "updated".getBytes());

        mockMvc.perform(multipart("/documents/notes.txt").file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(9));
    }

    @Test
    void replaceRejectsAMissingDocument() throws Exception {
        when(documentService.replace(eq("missing.txt"), any())).thenThrow(new DocumentNotFoundException("missing.txt"));
        MockMultipartFile file = new MockMultipartFile("file", "missing.txt", "text/plain", "x".getBytes());

        mockMvc.perform(multipart("/documents/missing.txt").file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeDeletesTheDocument() throws Exception {
        mockMvc.perform(delete("/documents/notes.txt"))
                .andExpect(status().isNoContent());

        verify(documentService).remove("notes.txt");
    }

    @Test
    void removeRejectsAMissingDocument() throws Exception {
        org.mockito.Mockito.doThrow(new DocumentNotFoundException("missing.txt"))
                .when(documentService).remove("missing.txt");

        mockMvc.perform(delete("/documents/missing.txt"))
                .andExpect(status().isNotFound());
    }
}
