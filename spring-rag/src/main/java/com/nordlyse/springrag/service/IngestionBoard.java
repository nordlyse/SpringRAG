package com.nordlyse.springrag.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
@Getter
@Setter
@NoArgsConstructor(onConstructor_ = @Autowired)
@AllArgsConstructor
public class IngestionBoard {

    private ConcurrentHashMap<String, IngestionNotice> notices = new ConcurrentHashMap<>();

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
                        .thenComparing(IngestionNotice::getFileName))
                .toList();
    }

    private static int rank(IngestionNotice notice) {
        return switch (notice.getState()) {
            case "scanning" -> 0;
            case "failed" -> 1;
            default -> 2;
        };
    }
}
