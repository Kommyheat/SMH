package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.CalendarItemResponse;
import org.example.dto.CalendarMonthResponse;
import org.example.service.MedicationCalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class MedicationCalendarController {

    private final MedicationCalendarService medicationCalendarService;

    //특정 날짜 상세 복약 리스트 조회
    @GetMapping("/day")
    public List<CalendarItemResponse> getDailyCalendar(
            @RequestParam Long userId,
            @RequestParam LocalDate date
    ) {
        return medicationCalendarService.getDailyCalendar(userId, date);
    }

    //월간 캘린더 조회
    @GetMapping("/month")
    public CalendarMonthResponse getMonthlyCalendar(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return medicationCalendarService.getMonthlyCalendar(userId, year, month);
    }
}
