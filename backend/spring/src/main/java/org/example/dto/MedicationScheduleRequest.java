package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.TimeSlot;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class MedicationScheduleRequest {

    private Long userId;
    private Long medicationId;

    private TimeSlot timeSlot;

    private LocalTime scheduledTime;

    private double quantity;

    private String unit;

    private boolean notificationEnabled;

    public boolean getNotificationEnabled() {
        return notificationEnabled;
    }
}
