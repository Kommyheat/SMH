package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.TodaySectionResponse;
import org.example.service.TodayMedicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class TodayMedicationController {

    private final TodayMedicationService todayMedicationService;

    //홈 화면 오늘 복약 리스트 조회 API
    @GetMapping("/today")
    public List<TodaySectionResponse> getTodayMedications(
            @RequestParam Long userId,

            // 날짜 미입력 시 오늘 날짜 자동 사용
            @RequestParam(required = false) LocalDate date
    ) {

        LocalDate targetDate = date != null
                ? date
                : LocalDate.now();

        return todayMedicationService.getTodayMedications(userId, targetDate);
    }
}
