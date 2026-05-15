package org.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "intake_logs")
public class IntakeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    @Column(nullable = false)
    private LocalDate date;

    private LocalDateTime takenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntakeStatus status;

    private String memo;

    public IntakeLog(MedicationSchedule medicationSchedule, LocalDate date) {
        validate(medicationSchedule, date);
        this.medicationSchedule = medicationSchedule;
        this.date = date;
        this.status = IntakeStatus.SCHEDULED;
    }

    public void markTaken() {
        this.status = IntakeStatus.TAKEN;
        this.takenAt = LocalDateTime.now();
    }

    public void markMissed() {
        this.status = IntakeStatus.MISSED;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    private void validate(MedicationSchedule medicationSchedule, LocalDate date) {
        if (medicationSchedule == null) {
            throw new IllegalArgumentException("복약 스케줄 정보는 필수입니다.");
        }
        if (date == null) {
            throw new IllegalArgumentException("복용 날짜는 필수입니다.");
        }
    }
}