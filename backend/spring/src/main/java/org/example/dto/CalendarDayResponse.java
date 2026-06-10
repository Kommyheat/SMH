package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.domain.CalenderDayStatus;

import java.time.LocalDate;


//달력에서 하루 한 칸 데이터
@Getter
@AllArgsConstructor
public class CalendarDayResponse {

    private LocalDate date;

    private CalenderDayStatus status;

    private int totalCount;

    private int takenCount;

    private int missedCount;
}
