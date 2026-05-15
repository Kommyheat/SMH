package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareLinkStatusResponse {
    private Long id;
    private String status;
    private Long caregiverId;
    private String caregiverName;
    private Long patientId;
    private String patientName;
    private String linkedAt;
    private String disconnectedAt;
}
