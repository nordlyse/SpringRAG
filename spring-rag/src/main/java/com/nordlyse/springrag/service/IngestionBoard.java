package com.nordlyse.springrag.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class IngestionBoard {

    private final ConcurrentHashMap<String, IngestionNotice> notices = new ConcurrentHashMap<>();

    public void scanning(String fileName) {
        notices.put(fileName, new IngestionNotice(fileName, "scanning", "Scanning " + fileName + "..."));
    }

    public void ready(String fileName) {
        notices.put(fileName, new IngestionNotice(
                fileName, "ready", fileName + " was scanned and is ready to use."));
    }

    public void failed(String fileName, String message) {
        notices.put(fileName, new IngestionNotice(fileName, "failed", message));
    }

    public void remove(String fileName) {
        notices.remove(fileName);
    }

    public List<IngestionNotice> notices() {
        return notices.values().stream()
                .sorted(Comparator
                        .comparingInt(IngestionBoard::rank)
                        .thenComparing(IngestionNotice::fileName))
                .toList();
    }

    private static int rank(IngestionNotice notice) {
        return switch (notice.state()) {
            case "scanning" -> 0;
            case "failed" -> 1;
            default -> 2;
        };
    }
}
