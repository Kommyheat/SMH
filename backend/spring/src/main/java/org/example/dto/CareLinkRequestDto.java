package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CareLinkRequestDto {
    private Long caregiverId;
    private String patientUserCode;
}
