package org.example.dto;

import lombok.Getter;
import org.example.domain.Medication;
import org.example.domain.MedicationStatus;

import java.time.LocalDate;

@Getter
public class MedicationResponse {

    private Long id;
    private Long userId;
    private String medicationName;
    private String ingredient;
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private MedicationStatus status;

    public MedicationResponse(Medication medication) {
        this.id = medication.getId();
        this.userId = medication.getUser().getId();
        this.medicationName = medication.getMedicationName();
        this.ingredient = medication.getIngredient();
        this.purpose = medication.getPurpose();
        this.startDate = medication.getStartDate();
        this.endDate = medication.getEndDate();
        this.status = medication.getStatus();
    }
}
