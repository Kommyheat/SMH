package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.MedicationScheduleRequest;
import org.example.dto.MedicationScheduleResponse;
import org.example.service.MedicationScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class MedicationScheduleController {

    private final MedicationScheduleService medicationScheduleService;

    @PostMapping
    public Long createSchedule(@RequestBody MedicationScheduleRequest request) {
        return medicationScheduleService.createSchedule(
                request.getUserId(),
                request
        );
    }

    @GetMapping("/medication/{medicationId}")
    public List<MedicationScheduleResponse> getMedicationSchedule(
            @PathVariable Long medicationId,
            @RequestParam Long userId
    ) {
        return medicationScheduleService.getSchedulesByMedication(userId, medicationId);
    }

    @GetMapping("/user/{userId}")
    public List<MedicationScheduleResponse> getSchedulesByUser(@PathVariable Long userId) {
        return medicationScheduleService.getSchedulesByUser(userId);
    }

    @PutMapping("/{scheduleId}")
    public String updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody MedicationScheduleRequest request
    ) {
        medicationScheduleService.updateSchedule(
                scheduleId,
                request.getUserId(),
                request
        );

        return "복약 일정이 수정되었습니다.";
    }

    @DeleteMapping("/{scheduleId}")
    public String deleteSchedule(
            @PathVariable Long scheduleId,
            @RequestParam Long userId
    ) {
        medicationScheduleService.deleteSchedule(scheduleId, userId);
        return "복약 일정이 삭제되었습니다.";
    }
}
