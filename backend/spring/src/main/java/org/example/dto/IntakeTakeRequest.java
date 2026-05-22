package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class IntakeTakeRequest {

    private Long userId;
    private LocalDate date;
    private String memo;
}
