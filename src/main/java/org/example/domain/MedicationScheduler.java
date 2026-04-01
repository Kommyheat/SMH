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
public class MedicationScheduler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 약의 복용 스케줄인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false)
    private LocalTime scheduledTime;   // 복용 시간

    @Column(nullable = false)
    private double quantity;           // 복용량

    @Column(nullable = false, length = 20)
    private String unit;               // 복용량 단위(정, ml, 포 등)

    @Column(nullable = false)
    private boolean notificationEnabled; // 알림 사용 여부

    public MedicationScheduler(Medication medication, LocalTime scheduledTime,
                              double quantity, String unit, boolean notificationEnabled) {
        validate(scheduledTime, quantity, unit);
        this.medication = medication;
        this.scheduledTime = scheduledTime;
        this.quantity = quantity;
        this.unit = unit;
        this.notificationEnabled = notificationEnabled;
    }

    public void updateSchedule(LocalTime scheduledTime, double quantity,
                               String unit, boolean notificationEnabled) {
        validate(scheduledTime, quantity, unit);
        this.scheduledTime = scheduledTime;
        this.quantity = quantity;
        this.unit = unit;
        this.notificationEnabled = notificationEnabled;
    }

    public void changeNotification(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    //오류 검증
    private void validate(LocalTime scheduledTime, double quantity, String unit) {
        if (scheduledTime == null) {
            throw new IllegalArgumentException("복용 시간은 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("복용량은 0보다 커야 합니다.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("복용 단위는 필수입니다.");
        }
    }
}