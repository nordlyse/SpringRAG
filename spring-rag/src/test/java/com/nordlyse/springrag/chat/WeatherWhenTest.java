package com.nordlyse.springrag.chat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherWhenTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    @Test
    void yesterdayIsTheDayBeforeToday() {
        WeatherWhen.Asked asked = WeatherWhen.resolve("Ankara'da dün hava nasıldı", TODAY);

        assertThat(asked.date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(asked.label()).isEqualTo("yesterday");
        assertThat(asked.kind()).isEqualTo(WeatherWhen.Kind.FORECAST);
        assertThat(PassageQuestion.city("Ankara'da dün hava nasıldı")).isEqualTo("ankara");
    }

    @Test
    void aCountOfDaysAgoKeepsTheCity() {
        WeatherWhen.Asked asked = WeatherWhen.resolve("Ankarada hava 2 gun once nasildi", TODAY);

        assertThat(asked.date()).isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(asked.label()).isEqualTo("2 days before today");
        assertThat(asked.kind()).isEqualTo(WeatherWhen.Kind.FORECAST);
        assertThat(PassageQuestion.city("Ankarada hava 2 gun once nasildi")).isEqualTo("ankara");
        assertThat(WeatherWhen.resolve("3 gün sonra ankara hava", TODAY).date()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(WeatherWhen.resolve("iki gün önce ankara hava", TODAY).label()).isEqualTo("2 days before today");
    }

    @Test
    void oncekiMeansBeforeTodayAndDropsTheWeatherWords() {
        WeatherWhen.Asked asked = WeatherWhen.resolve("Ankara da 3 gun onceki hava durumu nedir?", TODAY);

        assertThat(asked.date()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(asked.label()).isEqualTo("3 days before today");
        assertThat(asked.kind()).isEqualTo(WeatherWhen.Kind.FORECAST);
        assertThat(PassageQuestion.city("Ankara da 3 gun onceki hava durumu nedir?")).isEqualTo("ankara");
        assertThat(WeatherWhen.resolve("3 gun sonraki ankara hava", TODAY).label()).isEqualTo("3 days after today");
    }

    @Test
    void tomorrowAndTheDayAfterStayInTheForecastWindow() {
        assertThat(WeatherWhen.resolve("yarin ankara hava", TODAY).date()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(WeatherWhen.resolve("yarin ankara hava", TODAY).label()).isEqualTo("tomorrow");
        assertThat(WeatherWhen.resolve("obursugun ankara hava", TODAY).date()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(WeatherWhen.resolve("öbür gün ankara hava", TODAY).label()).isEqualTo("the day after tomorrow");
        assertThat(PassageQuestion.city("yarin ankara da hava nasil")).isEqualTo("ankara");
        assertThat(PassageQuestion.city("obursugun ankara hava")).isEqualTo("ankara");
    }

    @Test
    void writtenDatesUseDayMonthYear() {
        WeatherWhen.Asked asked = WeatherWhen.resolve("12.08.2002 ankara hava nasil", TODAY);

        assertThat(asked.date()).isEqualTo(LocalDate.of(2002, 8, 12));
        assertThat(asked.kind()).isEqualTo(WeatherWhen.Kind.ARCHIVE);
        assertThat(WeatherWhen.resolve("12/08/2002 ankara hava", TODAY).date()).isEqualTo(LocalDate.of(2002, 8, 12));
        assertThat(WeatherWhen.resolve("2002-08-12 ankara hava", TODAY).date()).isEqualTo(LocalDate.of(2002, 8, 12));
        assertThat(WeatherWhen.resolve("12 ağustos 2002 ankara hava", TODAY).date()).isEqualTo(LocalDate.of(2002, 8, 12));
        assertThat(PassageQuestion.city("12.08.2002 ankara da hava")).isEqualTo("ankara");
    }

    @Test
    void aDatePastTheForecastHorizonIsNotToday() {
        WeatherWhen.Asked ahead = WeatherWhen.resolve("01.12.2026 ankara hava", TODAY);
        WeatherWhen.Asked broken = WeatherWhen.resolve("31.02.2002 ankara hava", TODAY);

        assertThat(ahead.date()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(ahead.kind()).isEqualTo(WeatherWhen.Kind.OUTSIDE);
        assertThat(broken.kind()).isEqualTo(WeatherWhen.Kind.UNREADABLE);
        assertThat(WeatherWhen.kind(TODAY.minusDays(93), TODAY)).isEqualTo(WeatherWhen.Kind.ARCHIVE);
        assertThat(WeatherWhen.kind(TODAY.plusDays(16), TODAY)).isEqualTo(WeatherWhen.Kind.FORECAST);
    }
}
