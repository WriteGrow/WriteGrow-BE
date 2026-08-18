package com.example.writegrow.domain.report;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 리포트가 다루는 한 주. 월요일에 시작해 다음 월요일 직전까지다(ISO 8601).
 *
 * <p>주 경계 계산이 여러 곳에 흩어지면 "이번 주"와 "지난 주"가 미묘하게 어긋난다. 한 곳에 모은다.
 *
 * @param start 포함
 * @param end   <b>미포함</b>. 조회 조건이 {@code [start, end)} 라 경계의 하루가 두 주에 겹치지 않는다.
 */
public record WeekRange(LocalDate start, LocalDate end) {

    public static WeekRange of(LocalDate anyDayInWeek) {
        LocalDate monday = anyDayInWeek.with(DayOfWeek.MONDAY);
        return new WeekRange(monday, monday.plusWeeks(1));
    }

    public static WeekRange current() {
        return of(LocalDate.now());
    }

    public WeekRange previous() {
        return new WeekRange(start.minusWeeks(1), start);
    }

    /**
     * 화면에 표시할 마지막 날(일요일). {@link #end} 는 미포함 경계라 그대로 쓰면 하루 밀린다.
     */
    public LocalDate lastDay() {
        return end.minusDays(1);
    }

    public LocalDateTime startDateTime() {
        return start.atStartOfDay();
    }

    public LocalDateTime endDateTime() {
        return end.atStartOfDay();
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && date.isBefore(end);
    }
}
