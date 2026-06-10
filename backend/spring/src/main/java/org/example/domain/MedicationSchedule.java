package org.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "medication_schedules")
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 약의 복용 스케줄인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeSlot timeSlot;

    @Column(nullable = false)
    private LocalTime scheduledTime;   // 복용 시간

    @Column(nullable = false)
    private double quantity;           // 복용량

    @Column(nullable = false, length = 20)
    private String unit;               // 복용량 단위(정, ml, 포 등)

    @Column(nullable = false)
    private boolean notificationEnabled; // 알림 사용 여부

    public MedicationSchedule(
            Medication medication,
            TimeSlot timeSlot,
            LocalTime scheduledTime,
            double quantity,
            String unit,
            boolean notificationEnabled
    ) {
        validate(medication, timeSlot, scheduledTime, quantity, unit);
        this.medication = medication;
        this.timeSlot = timeSlot;
        this.scheduledTime = scheduledTime;
        this.quantity = quantity;
        this.unit = unit;
        this.notificationEnabled = notificationEnabled;
    }

    public void updateSchedule(
            TimeSlot timeSlot,
            LocalTime scheduledTime,
            double quantity,
            String unit,
            boolean notificationEnabled
    ) {
        validate(this.medication, timeSlot, scheduledTime, quantity, unit);
        this.timeSlot = timeSlot;
        this.scheduledTime = scheduledTime;
        this.quantity = quantity;
        this.unit = unit;
        this.notificationEnabled = notificationEnabled;
    }

    public void changeNotification(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    private void validate(
            Medication medication,
            TimeSlot timeSlot,
            LocalTime scheduledTime,
            double quantity,
            String unit
    ) {
        if (medication == null) {
            throw new IllegalArgumentException("복약 스케줄에 연결된 약 정보는 필수입니다.");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("복용 시간대는 필수입니다.");
        }
        if (scheduledTime == null) {
            throw new IllegalArgumentException("복용 시간은 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("복용량은 0보다 커야 합니다.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("복용 단위는 필수입니다.");
        }
        if (unit.length() > 20) {
            throw new IllegalArgumentException("복용 단위는 20자 이하여야 합니다.");
        }
    }
}
