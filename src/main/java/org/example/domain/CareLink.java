package org.example.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "care_links")
public class CareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //보호자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private User caregiver;

    //피보호자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareLinkStatus status;

    @Column(nullable = false)
    private LocalDate linkedDate;

    public CareLink(User caregiver, User patient) {
        validate(caregiver, patient);
        this.caregiver = caregiver;
        this.patient = patient;
        this.status = CareLinkStatus.PENDING;
        this.linkedDate = LocalDate.now();
    }
    public void accept() {
        this.status = CareLinkStatus.ACCEPTED;
    }

    public void reject() {
        this.status = CareLinkStatus.REJECTED;
    }

    public void disconnect() {
        this.status = CareLinkStatus.DISCONNECTED;
    }

    private void validate(User caregiver, User patient) {
        if (caregiver == null || patient == null) {
            throw new IllegalArgumentException("보호자와 피보호자 정보는 필수입니다.");
        }
        if (caregiver.equals(patient)) {
            throw new IllegalArgumentException("자기 자신과는 간병 연동을 할 수 없습니다.");
        }
    }
}
