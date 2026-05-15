package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PatientIntakeStatusResponse {

    private Long patientId;
    private String patientName;
    private List<IntakeItemResponse> IntakeLogs;
}
