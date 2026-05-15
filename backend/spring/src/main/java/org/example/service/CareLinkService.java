package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.CareLink;
import org.example.domain.CareLinkStatus;
import org.example.domain.User;
import org.example.dto.*;
import org.example.repository.CareLinkRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareLinkService {

    private final CareLinkRepository careLinkRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public CareLinkResponse connectPatient(Long caregiverId, CareLinkCreateRequest request) {
        User caregiver = findUserById(caregiverId);

        User patient = userRepository.findByLinkCode(request.getLinkCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고유 코드입니다."));

        if (caregiver.getId().equals(patient.getId())) {
            throw new IllegalArgumentException("본인과는 연동할 수 없습니다.");
        }

        boolean alreadyLinked = careLinkRepository.existsByCaregiverAndPatientAndStatus(
                caregiver, patient, CareLinkStatus.ACTIVE
        );

        if (alreadyLinked) {
            throw new IllegalArgumentException("이미 연동된 피보호자입니다.");
        }

        CareLink careLink = new CareLink(caregiver, patient);
        CareLink saved = careLinkRepository.save(careLink);

        return CareLinkResponse.from(saved);
    }

    public List<LinkedPatientResponse> getLinkedPatients(Long caregiverId) {
        User caregiver = findUserById(caregiverId);

        return careLinkRepository.findByCaregiverAndStatus(caregiver, CareLinkStatus.ACTIVE)
                .stream()
                .map(LinkedPatientResponse::from)
                .toList();
    }

    @Transactional
    public void disconnect(Long userId, Long linkId) {
        findUserById(userId);

        // findById 후 직접 권한 체크 (caregiver + patient 모두 가능)
        CareLink careLink = careLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("연결 정보를 찾을 수 없습니다."));

        // 해당 연동에 관련된 사용자인지 확인
        boolean isRelated = careLink.getCaregiver().getId().equals(userId)
                || careLink.getPatient().getId().equals(userId);

        if (!isRelated) {
            throw new IllegalArgumentException("해당 연동에 대한 권한이 없습니다.");
        }

        if (careLink.getStatus() != CareLinkStatus.ACTIVE) {
            throw new IllegalArgumentException("연동 중인 상태가 아닙니다.");
        }

        careLink.disconnect();
    }

    public void validateAccessToPatient(Long caregiverId, Long patientId) {
        User caregiver = findUserById(caregiverId);
        User patient = findUserById(patientId);

        boolean linked = careLinkRepository.existsByCaregiverAndPatientAndStatus(
                caregiver, patient, CareLinkStatus.ACTIVE
        );

        if (!linked) {
            throw new IllegalArgumentException("해당 피보호자에 대한 조회 권한이 없습니다.");
        }
    }

    // 이유: GET /api/care-links/status?userId= 대응
    // userId 기준으로 caregiver 또는 patient 로 연결된 가장 최근 상태 반환
    public CareLinkStatusResponse getCareLinkStatus(Long userId) {
        User user = findUserById(userId);
        List<CareLink> links = careLinkRepository.findByCaregiversOrPatient(user);

        if (links.isEmpty()) return null;

        CareLink link = links.get(0);

        return CareLinkStatusResponse.builder()
                .id(link.getId())
                .status(link.getStatus().name())
                .caregiverId(link.getCaregiver().getId())
                .caregiverName(link.getCaregiver().getName())
                .patientId(link.getPatient().getId())
                .patientName(link.getPatient().getName())
                .linkedAt(link.getLinkedAt() != null
                        ? link.getLinkedAt().format(FMT) : null)
                .disconnectedAt(link.getDisconnectedAt() != null
                        ? link.getDisconnectedAt().format(FMT) : null)
                .build();
    }

    // 추가: POST /api/care-links/request 대응
    // caregiverId + patientUserCode(=users.link_code) 로 연동 요청 생성
    @Transactional
    public MessageResponse requestCareLink(CareLinkRequestDto request) {
        User caregiver = findUserById(request.getCaregiverId());

        User patient = userRepository.findByLinkCode(request.getPatientUserCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고유 코드입니다."));

        if (caregiver.getId().equals(patient.getId())) {
            throw new IllegalArgumentException("본인과는 연동할 수 없습니다.");
        }

        // ACTIVE 상태 중복 방지
        boolean alreadyActive = careLinkRepository
                .existsByCaregiverAndPatientAndStatus(caregiver, patient, CareLinkStatus.ACTIVE);
        if (alreadyActive) {
            throw new IllegalArgumentException("이미 연동 중입니다.");
        }

        // PENDING 상태 중복 방지
        boolean alreadyPending = careLinkRepository
                .existsByCaregiverAndPatientAndStatus(caregiver, patient, CareLinkStatus.PENDING);
        if (alreadyPending) {
            throw new IllegalArgumentException("이미 연동 요청 중입니다.");
        }

        // 수정: DISCONNECTED 상태면 재활용 (새 레코드 생성 시 유니크 제약 오류 방지)
        java.util.Optional<CareLink> disconnected = careLinkRepository
                .findByCaregiverAndPatientAndStatus(caregiver, patient, CareLinkStatus.DISCONNECTED);

        if (disconnected.isPresent()) {
            disconnected.get().resetToPending();
            return new MessageResponse("연동 요청을 보냈습니다.");
        }

        // 수정: REJECTED 상태면 재활용
        java.util.Optional<CareLink> rejected = careLinkRepository
                .findByCaregiverAndPatientAndStatus(caregiver, patient, CareLinkStatus.REJECTED);

        if (rejected.isPresent()) {
            rejected.get().resetToPending();
            return new MessageResponse("연동 요청을 보냈습니다.");
        }

        // 기존 레코드 없으면 새로 생성
        CareLink careLink = new CareLink(caregiver, patient);
        careLinkRepository.save(careLink);

        return new MessageResponse("연동 요청을 보냈습니다.");
    }

    // 추가: POST /api/care-links/accept 대응
    // careLinkId로 레코드 찾아 status = ACTIVE, linkedAt 설정
    @Transactional
    public MessageResponse acceptCareLink(CareLinkDecisionRequest request) {
        CareLink link = careLinkRepository.findById(request.getCareLinkId())
                .orElseThrow(() -> new IllegalArgumentException("연동 요청을 찾을 수 없습니다."));

        link.accept();
        return new MessageResponse("연동 요청을 수락했습니다.");
    }

    // 추가: POST /api/care-links/reject 대응
    // careLinkId로 레코드 찾아 status = REJECTED
    @Transactional
    public MessageResponse rejectCareLink(CareLinkDecisionRequest request) {
        CareLink link = careLinkRepository.findById(request.getCareLinkId())
                .orElseThrow(() -> new IllegalArgumentException("연동 요청을 찾을 수 없습니다."));

        link.reject();
        return new MessageResponse("연동 요청을 거절했습니다.");
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
