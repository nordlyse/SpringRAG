package com.nordlyse.springrag.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;

final class PassageQuestion {

    private static final Pattern YEAR = Pattern.compile("\\b20\\d{2}\\b");
    private static final Pattern EMPLOYMENT_DATE = Pattern.compile(
            "(?i)\\b(jan|feb|mar|apr|may|mai|jun|jul|aug|sep|oct|okt|nov|dec|des)\\.?\\s*20\\d{2}\\b");
    private static final Pattern EMPLOYMENT_ROLE = Pattern.compile(
            "(?i).*(systemutvikler|utvikler|ingeniør|ingenior|dba)\\s*[–\\-].*");
    private static final Pattern NOT_EMPLOYMENT = Pattern.compile(
            "(?i).*(kurs|certif|sertif|feature|uteksaminert|akademi|høghskole|hoghskole).*");
    private static final Pattern STATED_NAME = Pattern.compile(
            "(?iu)(?:benim\\s+ad[ıi]m|ad[ıi]m|my\\s+name\\s+is|i\\s+am|ben)\\s+([\\p{L}']+)");

    static final String FILE_NAME = "file_name";

    private PassageQuestion() {
    }

    static String prompt(String question, List<Document> passages) {
        String body = passages == null
                ? ""
                : passages.stream()
                        .map(Document::getText)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(text -> !text.isEmpty())
                        .sorted(Comparator.comparingInt(PassageQuestion::rank))
                        .collect(Collectors.joining("\n"));
        String name = statedName(question);
        if (body.isEmpty()) {
            if (name != null) {
                return """
                        No stored document is about %s.
                        Do not list employers or places from any other person.

                        Question:
                        %s
                        """.formatted(name, question);
            }
            return """
                    No stored document matched this question.
                    Answer from your general knowledge.

                    Question:
                    %s
                    """.formatted(question);
        }
        if (asksForWorkplaces(question)) {
            String records = employmentRecords(body);
            if (!records.isBlank()) {
                String person = name == null ? "the user" : name;
                return """
                        These lines are employment records for %s only.
                        List every employer and place for %s, with the dates on the neighbouring lines.
                        Answer in the same language as the question.
                        Do not answer with only a year.
                        Do not include any other person.

                        Records:
                        %s

                        Question:
                        %s
                        """.formatted(person, person, records, question);
            }
        }
        return """
                The passages are the user's stored documents.
                Answer in the same language as the question.
                Use a passage only when it answers the question.

                Passages:
                %s

                Question:
                %s
                """.formatted(body, question);
    }

    static String city(String question) {
        if (question == null || question.isBlank()) {
            return "";
        }
        String normalized = question.toLowerCase(Locale.ROOT)
                .replace('\'', ' ')
                .replace('’', ' ')
                .replaceAll("[^\\p{L}\\s]", " ");
        Set<String> skip = Set.of(
                "bugun", "bugün", "today", "hava", "weather", "forecast", "nasil", "nasıl",
                "how", "what", "is", "the", "in", "a", "da", "de", "ta", "te", "icin", "için");
        List<String> kept = new ArrayList<>();
        for (String word : normalized.split("\\s+")) {
            if (word.isBlank() || skip.contains(word)) {
                continue;
            }
            if (word.length() > 4 && (word.endsWith("da") || word.endsWith("de") || word.endsWith("ta") || word.endsWith("te"))) {
                word = word.substring(0, word.length() - 2);
            }
            if (word.length() >= 2 && !skip.contains(word)) {
                kept.add(word);
            }
        }
        return String.join(" ", kept);
    }

    static String weatherPrompt(String question, String report) {
        return """
                This is the Open-Meteo weather report for the question.
                Answer in the same language as the question, in two short sentences.
                Use only this report.
                Do not mention tools.
                Do not invent a temperature.

                Report:
                %s

                Question:
                %s
                """.formatted(report, question);
    }

    static boolean asksForWeather(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        return lower.contains("hava")
                || lower.contains("weather")
                || lower.contains("forecast")
                || lower.contains("vær")
                || lower.contains("vaer");
    }

    static String statedName(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        Matcher matcher = STATED_NAME.matcher(question);
        if (!matcher.find()) {
            return null;
        }
        return personName(matcher.group(1));
    }

    static Set<String> filesAbout(String name, List<Document> passages) {
        Set<String> files = new LinkedHashSet<>();
        if (name == null || passages == null) {
            return files;
        }
        for (Document passage : passages) {
            String text = passage.getText();
            if (text == null || !mentions(text, name)) {
                continue;
            }
            Object fileName = passage.getMetadata().get(FILE_NAME);
            if (fileName != null && !fileName.toString().isBlank()) {
                files.add(fileName.toString());
            }
        }
        return files;
    }

    private static String personName(String token) {
        String word = token;
        int mark = word.indexOf('\'');
        if (mark < 0) {
            mark = word.indexOf('’');
        }
        if (mark > 1) {
            word = word.substring(0, mark);
        }
        word = word.replaceAll("[^\\p{L}]", "");
        if (word.length() >= 6 && word.matches("(?iu).+(?:im|ım|um|üm)$")) {
            word = word.substring(0, word.length() - 2);
        }
        if (word.length() < 2) {
            return null;
        }
        return word;
    }

    private static boolean mentions(String text, String name) {
        return Pattern.compile("(?iu)\\b" + Pattern.quote(name) + "\\b").matcher(text).find();
    }

    private static boolean asksForWorkplaces(String question) {
        if (asksForWeather(question)) {
            return false;
        }
        String lower = question.toLowerCase();
        return lower.contains("yer")
                || lower.contains("calis")
                || lower.contains("çalış")
                || lower.contains("work")
                || lower.contains("jobb")
                || lower.contains("arbeid");
    }

    private static String employmentRecords(String body) {
        return body.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(PassageQuestion::employmentLine)
                .collect(Collectors.joining("\n"));
    }

    private static boolean employmentLine(String line) {
        if (NOT_EMPLOYMENT.matcher(line).find()) {
            return false;
        }
        return EMPLOYMENT_DATE.matcher(line).find() || EMPLOYMENT_ROLE.matcher(line).find();
    }

    private static int rank(String text) {
        String lower = text.toLowerCase();
        boolean year = YEAR.matcher(lower).find();
        boolean workplace = lower.contains("arbeid")
                || lower.contains("utvikler")
                || lower.contains("employer")
                || lower.contains("worked");
        if (year && workplace) {
            return 0;
        }
        if (workplace) {
            return 1;
        }
        return 2;
    }
}
