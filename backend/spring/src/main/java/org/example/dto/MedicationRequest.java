package org.example.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MedicationRequest {

    private Long userId;
    private String medicationName;
    private String ingredient;
    private String purpose;

    private LocalDate startDate;
    private LocalDate endDate;
}
