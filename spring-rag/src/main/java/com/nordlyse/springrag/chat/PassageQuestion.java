package com.nordlyse.springrag.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
        if (body.isEmpty()) {
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
                return """
                        These lines are employment records from the user's documents.
                        List every employer and place, with the dates on the neighbouring lines.
                        Answer in the same language as the question.
                        Do not answer with only a year.

                        Records:
                        %s

                        Question:
                        %s
                        """.formatted(records, question);
            }
        }
        return """
                The passages are the user's stored documents.
                Answer in the same language as the question.
                When the question asks where the user worked, list every employer and place from the passages, together with the dates when they are present.
                Do not answer with only a year when an employer or a place is in the passages.

                Passages:
                %s

                Question:
                %s
                """.formatted(body, question);
    }

    private static boolean asksForWorkplaces(String question) {
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
