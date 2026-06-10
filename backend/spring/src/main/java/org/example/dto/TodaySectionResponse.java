package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.domain.TimeSlot;

import java.util.List;

@Getter
@AllArgsConstructor
public class TodaySectionResponse {

    private TimeSlot timeSlot;

    private List<TodayMedicationItemResponse> items;
}
