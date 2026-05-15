package org.example.dto;

import lombok.Getter;
import org.example.domain.CareLink;

@Getter
public class LinkedPatientResponse {

    private Long patientId;
    private String patientName;
    private String patientLoginId;

    public LinkedPatientResponse(CareLink careLink) {
        this.patientId = careLink.getPatient().getId();
        this.patientName = careLink.getPatient().getName();
        this.patientLoginId = careLink.getPatient().getLoginId();
    }

    public static LinkedPatientResponse from(CareLink careLink) {
        return new LinkedPatientResponse(careLink);
    }
}
