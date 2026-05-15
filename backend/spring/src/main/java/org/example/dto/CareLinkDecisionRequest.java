package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CareLinkDecisionRequest {
    private Long patientId;
    private Long careLinkId;
}
