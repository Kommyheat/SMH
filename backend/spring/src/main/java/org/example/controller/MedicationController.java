package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.domain.Medication;
import org.example.domain.MedicationStatus;
import org.example.dto.MedicationRequest;
import org.example.dto.MedicationResponse;
import org.example.dto.MedicationStatusRequest;
import org.example.service.MedicationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/medications")
public class MedicationController {

    private final MedicationService medicationService;

    //약 등록
    @PostMapping
    public Long createMedication(@RequestBody MedicationRequest request) {
        return medicationService.createMedication(
                request.getUserId(),
                request.getMedicationName(),
                request.getIngredient(),
                request.getPurpose(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    // 약 단건 조회
    @GetMapping("/{medicationId}")
    public MedicationResponse getMedication(@PathVariable Long medicationId) {
        Medication medication = medicationService.getMedication(medicationId);
        return new MedicationResponse(medication);
    }

    // 사용자별 전체 약 조회
    @GetMapping("/user/{userId}")
    public List<MedicationResponse> getMedicationsByUser(@PathVariable Long userId) {
        return medicationService.getMedicationsByUser(userId).stream()
                .map(MedicationResponse::new)
                .toList();
    }

    // 상태별 약 조회
    @GetMapping("/user/{userId}/status")
    public List<MedicationResponse> getMedicationsByStatus(
            @PathVariable Long userId,
            @RequestParam MedicationStatus status
    ) {
        return medicationService.getMedicationsByUserAndStatus(userId, status).stream()
                .map(MedicationResponse::new)
                .toList();
    }

    // 현재 복용중인 약 조회
    @GetMapping("/user/{userId}/current")
    public List<MedicationResponse> getCurrentMedications(@PathVariable Long userId) {
        return medicationService.getCurrentMedications(userId).stream()
                .map(MedicationResponse::new)
                .toList();
    }

    // 약 정보 수정
    @PutMapping("/{medicationId}")
    public String updateMedication(
            @PathVariable Long medicationId,
            @RequestBody MedicationRequest request
    ) {
        medicationService.updateMedication(
                medicationId,
                request.getUserId(),
                request.getMedicationName(),
                request.getIngredient(),
                request.getPurpose(),
                request.getStartDate(),
                request.getEndDate()
        );
        return "약 정보가 수정되었습니다.";
    }

    // 약 상태 변경
    @PatchMapping("/{medicationId}/status")
    public String changeMedicationStatus(
            @PathVariable Long medicationId,
            @RequestBody MedicationStatusRequest request
    ) {
        medicationService.changeMedicationStatus(
                medicationId,
                request.getUserId(),
                request.getStatus()
        );
        return "약 상태가 변경되었습니다.";
    }

    // 약 삭제
    @DeleteMapping("/{medicationId}")
    public String deleteMedication(
            @PathVariable Long medicationId,
            @RequestParam Long userId
    ) {
        medicationService.deleteMedication(medicationId, userId);
        return "약 정보가 삭제되었습니다.";
    }


}
