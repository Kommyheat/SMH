package org.example.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String medicationName;  //약물명

    @Column(length = 250)
    private String purpose;       //복용목적

    @Column(nullable = false)
    private LocalDate startDate;   //복용 시작일

    @Column(nullable = false)
    private LocalDate endDate;    //복용 종료일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicationStatus status;  //복용상태
    // endDate가 지나면 completed 처리를 할건지 아님 사용자가 직접 종료하게 할건지 정해야됨


    public Medication(User user, String medicationName, String purpose, LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.medicationName = medicationName;
        this.purpose = purpose;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = MedicationStatus.ACTIVE;  //기본적으로 복용중을 나타냄
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("복용 종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    public void updateMedicationInfo(String medicationName, String ingredient, String purpose,
                                     LocalDate startDate, LocalDate endDate) {
        this.medicationName = medicationName;
        this.purpose = purpose;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void changeStatus(MedicationStatus status) {
        this.status = status;
    }
}
