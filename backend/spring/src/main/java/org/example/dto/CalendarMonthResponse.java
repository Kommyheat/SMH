package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

//한달 전체 캘린더 응답
@Getter
@AllArgsConstructor
public class CalendarMonthResponse {

    private int year;

    private int month;

    private List<CalendarDayResponse> days;
}
