package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.domain.IntakeStatus;
import org.example.domain.TimeSlot;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class TodayMedicationItemResponse {

    private Long scheduleId;
    private Long medicationId;
    private String medicationName;

    private TimeSlot timeSlot;
    private LocalTime scheduledTime;

    private double quantity;
    private String unit;

    private IntakeStatus status;
    private boolean taken;
    private LocalDateTime takenAt;
}
