package com.nordlyse.springrag.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.extractor.SlideShowExtractor;

final class DocumentText {

    private DocumentText() {
    }

    static String read(Path path) throws IOException {
        return switch (extension(path)) {
            case "txt", "csv" -> Files.readString(path);
            case "pdf" -> pdf(path);
            case "docx", "xlsx", "pptx" -> officeXml(path);
            case "doc" -> doc(path);
            case "xls" -> xls(path);
            case "ppt" -> ppt(path);
            default -> "";
        };
    }

    private static String pdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String doc(Path path) throws IOException {
        try (WordExtractor extractor = new WordExtractor(new POIFSFileSystem(path.toFile()))) {
            return extractor.getText();
        }
    }

    private static String xls(Path path) throws IOException {
        try (ExcelExtractor extractor = new ExcelExtractor(new POIFSFileSystem(path.toFile()))) {
            return extractor.getText();
        }
    }

    private static String ppt(Path path) throws IOException {
        try (POIFSFileSystem fileSystem = new POIFSFileSystem(path.toFile());
                HSLFSlideShow slideShow = new HSLFSlideShow(fileSystem);
                SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
            return extractor.getText();
        }
    }

    private static String officeXml(Path path) throws IOException {
        StringBuilder text = new StringBuilder();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!officeEntry(entry.getName())) {
                    continue;
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    text.append(plainXml(new String(input.readAllBytes(), StandardCharsets.UTF_8)));
                    text.append('\n');
                }
            }
        }
        return text.toString();
    }

    private static boolean officeEntry(String name) {
        return name.equals("word/document.xml")
                || name.startsWith("word/header")
                || name.startsWith("word/footer")
                || name.equals("word/footnotes.xml")
                || name.equals("word/endnotes.xml")
                || name.equals("xl/sharedStrings.xml")
                || name.startsWith("xl/worksheets/")
                || name.startsWith("ppt/slides/")
                || name.startsWith("ppt/notesSlides/");
    }

    private static String plainXml(String xml) {
        String withBreaks = xml.replace("</w:p>", "\n")
                .replace("</a:p>", "\n")
                .replace("</si>", "\n")
                .replace("</t>", " ");
        String stripped = withBreaks.replaceAll("<[^>]+>", " ");
        return stripped.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
