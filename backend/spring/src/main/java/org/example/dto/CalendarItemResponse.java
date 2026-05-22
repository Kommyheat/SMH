package org.example.dto;

import lombok.Getter;
import org.example.domain.IntakeStatus;
import org.example.domain.TimeSlot;

import java.time.LocalDateTime;
import java.time.LocalTime;

//달력에서 날짜 클릭하면 나오는 상세 리스트
@Getter
public class CalendarItemResponse {

    private Long scheduleId;
    private Long medicationId;
    private String medicationName;

    private TimeSlot timeSlot;
    private LocalTime scheduledTime;

    private double quantity;
    private String unit;

    private IntakeStatus status;
    private LocalDateTime takenAt;
    private String memo;

    public CalendarItemResponse(
            Long scheduleId,
            Long medicationId,
            String medicationName,
            TimeSlot timeSlot,
            LocalTime scheduledTime,
            double quantity,
            String unit,
            IntakeStatus status,
            LocalDateTime takenAt,
            String memo
    ) {
        this.scheduleId = scheduleId;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.timeSlot = timeSlot;
        this.scheduledTime = scheduledTime;
        this.quantity = quantity;
        this.unit = unit;
        this.status = status;
        this.takenAt = takenAt;
        this.memo = memo;
    }
}
