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
@Table(
        name = "intake_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_intake_schedule_date",
                        columnNames = {"medication_schedule_id", "date"}
                )
        }
)
public class IntakeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 복약 스케줄에 대한 기록인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_schedule_id", nullable = false)
    private MedicationSchedule medicationSchedule;

    // 복약 예정 날짜
    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntakeStatus status;

    private LocalDateTime takenAt;

    @Column(length = 255)
    private String memo;

    public IntakeLog(MedicationSchedule medicationSchedule, LocalDate date) {
        if (medicationSchedule == null) {
            throw new IllegalArgumentException("복약 스케줄은 필수입니다.");
        }
        if (date == null) {
            throw new IllegalArgumentException("복약 날짜는 필수입니다.");
        }

        this.medicationSchedule = medicationSchedule;
        this.date = date;
        this.status = IntakeStatus.SCHEDULED;
    }

    public void markAsTaken(String memo) {
        this.status = IntakeStatus.TAKEN;
        this.takenAt = LocalDateTime.now();
        this.memo = memo;
    }

    public void cancelTaken() {
        this.status = IntakeStatus.SCHEDULED;
        this.takenAt = null;
        this.memo = null;
    }

    public void markAsMissed() {
        this.status = IntakeStatus.MISSED;
        this.takenAt = null;
    }
}
