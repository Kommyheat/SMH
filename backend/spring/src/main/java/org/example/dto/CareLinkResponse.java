package org.example.dto;

import lombok.Getter;
import org.example.domain.CareLink;

//보호자가 피보호자의 코드를 입력하고 연결 응답
@Getter
public class CareLinkResponse {

    private Long linkId;
    private Long patientId;
    private String patientName;
    private String patientLoginId;
    private String status;

    public CareLinkResponse(CareLink careLink) {
        this.linkId = careLink.getId();
        this.patientId = careLink.getPatient().getId();
        this.patientName = careLink.getPatient().getName();
        this.patientLoginId = careLink.getPatient().getLoginId();
        this.status = careLink.getStatus().name();
    }

    public static CareLinkResponse from(CareLink careLink) {
        return new CareLinkResponse(careLink);
    }
}