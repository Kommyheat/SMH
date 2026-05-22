package org.example.dto;

import lombok.Getter;
import org.example.domain.MedicationSchedule;
import org.example.domain.TimeSlot;

import java.time.LocalTime;

@Getter
public class MedicationScheduleResponse {

    private Long id;

    private Long medicationId;

    private String medicationName;

    private TimeSlot timeSlot;

    private LocalTime scheduledTime;

    private double quantity;

    private String unit;

    private boolean notificationEnabled;

    public MedicationScheduleResponse(MedicationSchedule medicationSchedule) {

        this.id = medicationSchedule.getId();

        this.medicationId = medicationSchedule.getMedication().getId();

        this.medicationName = medicationSchedule.getMedication().getMedicationName();

        this.timeSlot = medicationSchedule.getTimeSlot();

        this.scheduledTime = medicationSchedule.getScheduledTime();

        this.quantity = medicationSchedule.getQuantity();

        this.unit = medicationSchedule.getUnit();

        this.notificationEnabled = medicationSchedule.isNotificationEnabled();
    }
}
