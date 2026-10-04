package com.nordlyse.springrag.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextTest {

    @TempDir
    Path directory;

    @Test
    void readKeepsPlainText() throws Exception {
        Path notes = directory.resolve("notes.txt");
        Files.writeString(notes, "Worked at Nordlyse in Oslo.");

        assertThat(DocumentText.read(notes)).contains("Nordlyse in Oslo");
    }

    @Test
    void readKeepsTextFromAnOfficeXmlFile() throws Exception {
        Path document = directory.resolve("cv.docx");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(document))) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("""
                    <w:document><w:p><w:t>Worked at Nordlyse in Oslo.</w:t></w:p></w:document>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThat(DocumentText.read(document)).contains("Nordlyse in Oslo");
    }
}
