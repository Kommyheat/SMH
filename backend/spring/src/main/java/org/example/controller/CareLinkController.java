package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.service.CareLinkService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/care-links")
public class CareLinkController {

    private final CareLinkService careLinkService;

    // 기존 유지
    @PostMapping
    public CareLinkResponse connectPatient(@RequestParam Long caregiverId,
                                           @Valid @RequestBody CareLinkCreateRequest request) {
        return careLinkService.connectPatient(caregiverId, request);
    }

    @GetMapping
    public List<LinkedPatientResponse> getLinkedPatients(@RequestParam Long caregiverId) {
        return careLinkService.getLinkedPatients(caregiverId);
    }

    @DeleteMapping("/{linkId}")
    public MessageResponse disconnect(@RequestParam Long caregiverId,
                                      @PathVariable Long linkId) {
        // caregiverId 파라미터를 userId로 활용 (caregiver 또는 patient 모두 가능)
        careLinkService.disconnect(caregiverId, linkId);
        return new MessageResponse("연동이 해제되었습니다.");
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam Long userId) {
        CareLinkStatusResponse response = careLinkService.getCareLinkStatus(userId);
        if (response == null) {
            return ResponseEntity.ok(new MessageResponse("연동된 보호자가 없습니다."));
        }
        return ResponseEntity.ok(response);
    }

    // 연동 요청
    @PostMapping("/request")
    public MessageResponse requestLink(@RequestBody CareLinkRequestDto request) {
        return careLinkService.requestCareLink(request);
    }

    // 연동 수락
    @PostMapping("/accept")
    public MessageResponse acceptLink(@RequestBody CareLinkDecisionRequest request) {
        return careLinkService.acceptCareLink(request);
    }

    // 연동 거절
    @PostMapping("/reject")
    public MessageResponse rejectLink(@RequestBody CareLinkDecisionRequest request) {
        return careLinkService.rejectCareLink(request);
    }
}
