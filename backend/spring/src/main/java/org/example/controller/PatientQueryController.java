package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.PatientIntakeStatusResponse;
import org.example.service.CareLinkService;
import org.example.service.IntakeQueryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patients")
public class PatientQueryController {

    private final CareLinkService careLinkService;
    private final IntakeQueryService intakeQueryService;

    @GetMapping("/{patientId}/intake-status")
    public PatientIntakeStatusResponse getPatientIntakeStatus(@RequestParam Long caregiverId,
                                                              @PathVariable Long patientId) {
        careLinkService.validateAccessToPatient(caregiverId, patientId);
        return intakeQueryService.getTodayPatientIntakeStatus(patientId);
    }
}