package org.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "care_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_caregiver_patient",
                        columnNames = {"caregiver_id", "patient_id"})
        }
)
public class CareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 보호자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private User caregiver;

    // 피보호자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareLinkStatus status;

    // 수정: DB가 NULL 허용이므로 nullable = true (기본값)
    @Column
    private LocalDateTime linkedAt;

    // 수정: DB가 NULL 허용이므로 nullable = true (기본값)
    @Column
    private LocalDateTime disconnectedAt;

    // 수정: 생성 시 PENDING으로 시작 (기존 ACTIVE → PENDING)
    // 이유: 연동 요청 후 상대방이 수락해야 ACTIVE가 되는 흐름
    public CareLink(User caregiver, User patient) {
        validate(caregiver, patient);
        this.caregiver = caregiver;
        this.patient = patient;
        this.status = CareLinkStatus.PENDING;
        this.linkedAt = null;
        this.disconnectedAt = null;
    }

    // 추가: 수락 → ACTIVE + linkedAt 설정
    public void accept() {
        this.status = CareLinkStatus.ACTIVE;
        this.linkedAt = LocalDateTime.now();
    }

    // 추가: 거절 → REJECTED
    public void reject() {
        this.status = CareLinkStatus.REJECTED;
    }

    //  해제 → DISCONNECTED + disconnectedAt 설정
    public void disconnect() {
        this.status = CareLinkStatus.DISCONNECTED;
        this.disconnectedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == CareLinkStatus.ACTIVE;
    }

    private void validate(User caregiver, User patient) {
        if (caregiver == null || patient == null) {
            throw new IllegalArgumentException("보호자와 피보호자 정보는 필수입니다.");
        }
        if (caregiver.getId() != null && caregiver.getId().equals(patient.getId())) {
            throw new IllegalArgumentException("자기 자신과는 간병 연동을 할 수 없습니다.");
        }
    }
    // DISCONNECTED/REJECTED → PENDING 으로 재활용
    public void resetToPending() {
        this.status = CareLinkStatus.PENDING;
        this.linkedAt = null;
        this.disconnectedAt = null;
    }
}

