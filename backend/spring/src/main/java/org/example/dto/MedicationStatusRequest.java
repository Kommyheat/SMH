package org.example.dto;

import lombok.Getter;
import org.example.domain.MedicationStatus;

@Getter
public class MedicationStatusRequest {

    private Long userId;
    private MedicationStatus status;
}
