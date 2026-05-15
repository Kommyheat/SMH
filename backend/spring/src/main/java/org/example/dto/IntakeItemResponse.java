package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class IntakeItemResponse {

    private String medicationName;
    private LocalTime scheduledTime;
    private String status;  //IntakeStatus(Domain Enum)
    private LocalDateTime takenAt;
    private String memo;
    private double quantity;
    private String unit;
}