package org.example.dto;

import lombok.Getter;
import org.example.domain.IntakeLog;
import org.example.domain.IntakeStatus;
import org.example.domain.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class IntakeLogResponse {

    private Long intakeLogId;
    private Long scheduleId;
    private Long medicationId;
    private String medicationName;

    private LocalDate date;
    private LocalTime scheduledTime;

    private TimeSlot timeSlot;

    private double quantity;
    private String unit;

    private IntakeStatus status;
    private LocalDateTime takenAt;
    private String memo;

    public IntakeLogResponse(IntakeLog log) {
        this.intakeLogId = log.getId();
        this.scheduleId = log.getMedicationSchedule().getId();
        this.medicationId = log.getMedicationSchedule().getMedication().getId();
        this.medicationName = log.getMedicationSchedule().getMedication().getMedicationName();

        this.date = log.getDate();
        this.scheduledTime = log.getMedicationSchedule().getScheduledTime();

        this.timeSlot = log.getMedicationSchedule().getTimeSlot();

        this.quantity = log.getMedicationSchedule().getQuantity();
        this.unit = log.getMedicationSchedule().getUnit();

        this.status = log.getStatus();
        this.takenAt = log.getTakenAt();
        this.memo = log.getMemo();
    }
}
