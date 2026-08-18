package com.example.writegrow.domain.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeekRange 단위 테스트")
class WeekRangeTest {

    // 2026-08-18 은 화요일이다.
    private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 18);

    @Test
    @DisplayName("주중 아무 날짜를 줘도 그 주 월요일에서 시작한다")
    void startsOnMonday() {
        WeekRange week = WeekRange.of(TUESDAY);

        assertThat(week.start()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(week.end()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    @Test
    @DisplayName("월요일을 주면 그 주가 그대로 나온다")
    void keepsMonday() {
        assertThat(WeekRange.of(LocalDate.of(2026, 8, 17)).start())
                .isEqualTo(LocalDate.of(2026, 8, 17));
    }

    @Test
    @DisplayName("일요일은 그 주에 포함된다")
    void includesSunday() {
        WeekRange week = WeekRange.of(TUESDAY);

        assertThat(week.contains(LocalDate.of(2026, 8, 23))).isTrue();
        assertThat(week.contains(LocalDate.of(2026, 8, 24))).isFalse();
    }

    @Test
    @DisplayName("표시용 마지막 날은 일요일이다")
    void lastDayIsSunday() {
        assertThat(WeekRange.of(TUESDAY).lastDay()).isEqualTo(LocalDate.of(2026, 8, 23));
    }

    @Test
    @DisplayName("지난 주는 이번 주 시작 직전 한 주다")
    void previousWeekEndsWhereThisWeekStarts() {
        WeekRange week = WeekRange.of(TUESDAY);
        WeekRange previous = week.previous();

        assertThat(previous.start()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(previous.end()).isEqualTo(week.start());
        // 경계가 겹치지 않아야 같은 글이 두 주에 잡히지 않는다.
        assertThat(previous.contains(week.start())).isFalse();
    }
}
