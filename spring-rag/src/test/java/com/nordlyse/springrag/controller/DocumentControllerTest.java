package com.nordlyse.springrag.controller;

import java.util.List;

import com.nordlyse.springrag.service.DocumentStorageService;
import com.nordlyse.springrag.service.StoredDocument;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private DocumentStorageService documentStorageService;

    @Test
    void addAcceptsMultipartFile() throws Exception {
        when(documentStorageService.add(any())).thenReturn(new StoredDocument("notes.txt", 5, "text/plain"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/documents").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].fileName").value("notes.txt"))
                .andExpect(jsonPath("$[0].size").value(5));
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
}
