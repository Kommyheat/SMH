package org.example.domain;

import jakarta.persistence.*;
import jdk.jfr.Enabled;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "intake_logs")
public class IntakeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationScheduler medicationScheduler;


    @Column(nullable = false)
    private LocalDate date;  // 복용 날짜

    private LocalDateTime takenAt; // 실제 복용 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntakeStatus status;

    private String memo;

    public IntakeLog(MedicationScheduler scheduler, LocalDate date) {
        this.medicationScheduler = scheduler;
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
}
