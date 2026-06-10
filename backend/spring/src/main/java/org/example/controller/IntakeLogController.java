package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.IntakeLogResponse;
import org.example.dto.IntakeTakeRequest;
import org.example.service.IntakeLogService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intakes")
public class IntakeLogController {

    private final IntakeLogService intakeLogService;

    @PostMapping("/{scheduleId}/take")
    public Long takeMedication(
            @PathVariable Long scheduleId,
            @RequestBody IntakeTakeRequest request
    ) {
        return intakeLogService.takeMedication(
                scheduleId,
                request.getUserId(),
                request.getDate(),
                request.getMemo()
        );
    }

    @PostMapping("/{scheduleId}/cancel")
    public String cancelTake(
            @PathVariable Long scheduleId,
            @RequestBody IntakeTakeRequest request
    ) {
        intakeLogService.cancelTake(
                scheduleId,
                request.getUserId(),
                request.getDate()
        );

        return "복약 완료가 취소되었습니다.";
    }

    @GetMapping("/users/{userId}/daily")
    public List<IntakeLogResponse> getDailyLogs(
            @PathVariable Long userId,
            @RequestParam LocalDate date
    ) {
        return intakeLogService.getDailyLogs(userId, date);
    }

    @GetMapping("/users/{userId}/monthly")
    public List<IntakeLogResponse> getMonthlyLogs(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return intakeLogService.getMonthlyLogs(userId, year, month);
    }
}
