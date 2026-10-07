package com.nordlyse.springrag.chat;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
final class WeatherWhen {

    static final int FORECAST_PAST_DAYS = 92;
    static final int FORECAST_FUTURE_DAYS = 16;
    static final LocalDate ARCHIVE_START = LocalDate.of(1940, 1, 1);

    enum Kind {
        FORECAST,
        ARCHIVE,
        OUTSIDE,
        UNREADABLE
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class Asked {

        private LocalDate date;
        private String label;
        private Kind kind;

        static Asked unreadable() {
            return new Asked(null, "", Kind.UNREADABLE);
        }
    }

    private static final Pattern ISO = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern NUMERIC = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b");
    private static final Pattern DAY_MONTH = Pattern.compile("\\b(\\d{1,2})\\s+([a-z]+)\\s+(\\d{4})\\b");
    private static final Pattern MONTH_DAY = Pattern.compile("\\b([a-z]+)\\s+(\\d{1,2})\\s+(\\d{4})\\b");
    private static final Pattern BEFORE = Pattern.compile(
            "\\b(day before yesterday|evvelsi gun|evvelki gun|onceki gun)\\b");
    private static final Pattern AFTER = Pattern.compile(
            "\\b(day after tomorrow|obursu gun|obur gun|obursugun|oburgun|i overmorgen|overmorgen)\\b");
    private static final Pattern YESTERDAY = Pattern.compile("\\b(yesterday|dun|igar|i gar)\\b");
    private static final Pattern TOMORROW = Pattern.compile("\\b(tomorrow|yarin|imorgen|i morgen)\\b");
    private static final String COUNT = "\\d+|bir|iki|uc|dort|bes|alti|yedi|sekiz|dokuz|on";
    private static final Pattern COUNTED_AGO = Pattern.compile(
            "\\b(" + COUNT + ")\\s+(?:gun|days?)\\s+(?:onceki|once|ago)\\b");
    private static final Pattern COUNTED_AHEAD = Pattern.compile(
            "\\b(" + COUNT + ")\\s+(?:gun|days?)\\s+(?:sonraki|sonra|later|ahead)\\b");
    private static final Pattern DATE_WORD = Pattern.compile(
            "\\b(day before yesterday|day after tomorrow|evvelsi gun|evvelki gun|onceki gun|obursu gun|obur gun|i overmorgen|i morgen|i gar|obursugun|oburgun|yesterday|tomorrow|today|dun|yarin|bugun|evvelsi|evvelki|onceki|sonraki|obursu|obur|overmorgen|imorgen|igar|gun|guns|day|days|before|after|once|sonra|ago|later|ahead)\\b");

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("ocak", 1),
            Map.entry("subat", 2),
            Map.entry("mart", 3),
            Map.entry("nisan", 4),
            Map.entry("mayis", 5),
            Map.entry("haziran", 6),
            Map.entry("temmuz", 7),
            Map.entry("agustos", 8),
            Map.entry("eylul", 9),
            Map.entry("ekim", 10),
            Map.entry("kasim", 11),
            Map.entry("aralik", 12),
            Map.entry("january", 1),
            Map.entry("february", 2),
            Map.entry("march", 3),
            Map.entry("april", 4),
            Map.entry("may", 5),
            Map.entry("june", 6),
            Map.entry("july", 7),
            Map.entry("august", 8),
            Map.entry("september", 9),
            Map.entry("october", 10),
            Map.entry("november", 11),
            Map.entry("december", 12),
            Map.entry("januar", 1),
            Map.entry("februar", 2),
            Map.entry("mars", 3),
            Map.entry("mai", 5),
            Map.entry("juni", 6),
            Map.entry("juli", 7),
            Map.entry("oktober", 10),
            Map.entry("desember", 12));

    static Asked resolve(String question, LocalDate today) {
        String folded = fold(question);
        Parsed parsed = explicit(folded);
        if (parsed.isUnreadable()) {
            return Asked.unreadable();
        }
        if (parsed.getDate() != null) {
            return new Asked(parsed.getDate(), parsed.getDate().toString(), kind(parsed.getDate(), today));
        }
        Offset offset = offset(folded);
        LocalDate date = today.plusDays(offset.getDays());
        return new Asked(date, offset.getLabel(), kind(date, today));
    }

    static String withoutDates(String question) {
        String text = fold(question);
        text = COUNTED_AGO.matcher(text).replaceAll(" ");
        text = COUNTED_AHEAD.matcher(text).replaceAll(" ");
        text = DATE_WORD.matcher(text).replaceAll(" ");
        text = ISO.matcher(text).replaceAll(" ");
        text = NUMERIC.matcher(text).replaceAll(" ");
        text = stripNamedDates(text, DAY_MONTH, 2);
        text = stripNamedDates(text, MONTH_DAY, 1);
        return text;
    }

    static Kind kind(LocalDate date, LocalDate today) {
        if (date.isBefore(ARCHIVE_START)) {
            return Kind.OUTSIDE;
        }
        if (date.isBefore(today.minusDays(FORECAST_PAST_DAYS))) {
            return Kind.ARCHIVE;
        }
        if (date.isAfter(today.plusDays(FORECAST_FUTURE_DAYS))) {
            return Kind.OUTSIDE;
        }
        return Kind.FORECAST;
    }

    private static Offset offset(String folded) {
        Matcher ago = COUNTED_AGO.matcher(folded);
        if (ago.find()) {
            return counted(-count(ago.group(1)));
        }
        Matcher ahead = COUNTED_AHEAD.matcher(folded);
        if (ahead.find()) {
            return counted(count(ahead.group(1)));
        }
        if (BEFORE.matcher(folded).find()) {
            return new Offset(-2, "the day before yesterday");
        }
        if (AFTER.matcher(folded).find()) {
            return new Offset(2, "the day after tomorrow");
        }
        if (YESTERDAY.matcher(folded).find()) {
            return new Offset(-1, "yesterday");
        }
        if (TOMORROW.matcher(folded).find()) {
            return new Offset(1, "tomorrow");
        }
        return new Offset(0, "today");
    }

    private static Offset counted(int days) {
        if (days == 0) {
            return new Offset(0, "today");
        }
        if (days == -1) {
            return new Offset(-1, "yesterday");
        }
        if (days == 1) {
            return new Offset(1, "tomorrow");
        }
        if (days < 0) {
            return new Offset(days, Math.abs(days) + " days before today");
        }
        return new Offset(days, days + " days after today");
    }

    private static int count(String token) {
        return switch (token) {
            case "bir" -> 1;
            case "iki" -> 2;
            case "uc" -> 3;
            case "dort" -> 4;
            case "bes" -> 5;
            case "alti" -> 6;
            case "yedi" -> 7;
            case "sekiz" -> 8;
            case "dokuz" -> 9;
            case "on" -> 10;
            default -> Integer.parseInt(token);
        };
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Offset {

        private int days;
        private String label;
    }

    private static Parsed explicit(String folded) {
        Parsed earliest = null;
        earliest = earlier(earliest, iso(folded));
        earliest = earlier(earliest, numeric(folded));
        earliest = earlier(earliest, named(DAY_MONTH.matcher(folded), true));
        earliest = earlier(earliest, named(MONTH_DAY.matcher(folded), false));
        return earliest == null ? new Parsed(null, false, Integer.MAX_VALUE) : earliest;
    }

    private static Parsed earlier(Parsed current, Parsed next) {
        if (next == null) {
            return current;
        }
        if (current == null || next.getAt() < current.getAt()) {
            return next;
        }
        return current;
    }

    private static Parsed iso(String folded) {
        Matcher matcher = ISO.matcher(folded);
        if (!matcher.find()) {
            return null;
        }
        LocalDate date = date(group(matcher, 1), group(matcher, 2), group(matcher, 3));
        if (date == null) {
            return new Parsed(null, true, matcher.start());
        }
        return new Parsed(date, false, matcher.start());
    }

    private static Parsed numeric(String folded) {
        Matcher matcher = NUMERIC.matcher(folded);
        if (!matcher.find()) {
            return null;
        }
        int first = group(matcher, 1);
        int second = group(matcher, 2);
        int year = group(matcher, 3);
        LocalDate date = date(year, second, first);
        if (date == null) {
            date = date(year, first, second);
        }
        if (date == null) {
            return new Parsed(null, true, matcher.start());
        }
        return new Parsed(date, false, matcher.start());
    }

    private static Parsed named(Matcher matcher, boolean dayFirst) {
        Parsed found = null;
        while (matcher.find()) {
            Integer month = MONTHS.get(matcher.group(dayFirst ? 2 : 1));
            if (month == null) {
                continue;
            }
            int day = group(matcher, dayFirst ? 1 : 2);
            int year = group(matcher, 3);
            LocalDate date = date(year, month, day);
            Parsed parsed = date == null
                    ? new Parsed(null, true, matcher.start())
                    : new Parsed(date, false, matcher.start());
            found = earlier(found, parsed);
        }
        return found;
    }

    private static String stripNamedDates(String text, Pattern pattern, int monthGroup) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String replacement = MONTHS.containsKey(matcher.group(monthGroup)) ? " " : matcher.group();
            matcher.appendReplacement(out, replacement);
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static int group(Matcher matcher, int index) {
        return Integer.parseInt(matcher.group(index));
    }

    private static LocalDate date(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        }
        catch (DateTimeException ignored) {
            return null;
        }
    }

    private static String fold(String question) {
        if (question == null || question.isBlank()) {
            return "";
        }
        String lower = question.toLowerCase(Locale.forLanguageTag("tr"))
                .replace('\'', ' ')
                .replace('’', ' ')
                .replace('`', ' ');
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            out.append(switch (lower.charAt(i)) {
                case 'ş' -> 's';
                case 'ğ' -> 'g';
                case 'ü' -> 'u';
                case 'ö' -> 'o';
                case 'ç' -> 'c';
                case 'ı' -> 'i';
                case 'å' -> 'a';
                case 'ø' -> 'o';
                case 'æ' -> 'a';
                default -> lower.charAt(i);
            });
        }
        return out.toString();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Parsed {

        private LocalDate date;
        private boolean unreadable;
        private int at;
    }
}
