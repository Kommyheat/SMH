package org.example.dto;

import lombok.Getter;

@Getter
public class CareLinkResponse {

    private Long id;
    private String caregiverName;
    private String patientName;
    private String status;

    public CareLinkResponse(Long id, String caregiverName, String patientName, String status) {
        this.id = id;
        this.caregiverName = caregiverName;
        this.patientName = patientName;
        this.status = status;
    }
}
