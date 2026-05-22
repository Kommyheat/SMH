package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.IntakeLog;
import org.example.domain.IntakeStatus;
import org.example.domain.MedicationSchedule;
import org.example.domain.TimeSlot;
import org.example.dto.TodayMedicationItemResponse;
import org.example.dto.TodaySectionResponse;
import org.example.repository.IntakeLogRepository;
import org.example.repository.MedicationScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayMedicationService {

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final IntakeLogRepository intakeLogRepository;

    //홈 화면의 복약 리스트 조회
    //오늘 기준 활성화된 복약 스케줄 조회
    //아침/점심/저녁/취침 기준으로 그룹핑
    public List<TodaySectionResponse> getTodayMedications(Long userId, LocalDate date) {

        // 오늘 기준 활성화된 복약 스케줄 조회
        List<MedicationSchedule> schedules =
                medicationScheduleRepository.findActiveSchedulesByUserIdAndDate(userId, date);

        // 오늘 날짜의 복약 기록 조회
        List<IntakeLog> logs =
                intakeLogRepository.findByMedicationSchedule_Medication_User_IdAndDate(userId, date);

        //scheduleId 기준으로 빠르게 복약 로그 조회하기 위한 Map 생성 (key   = scheduleId, value = IntakeLog)
        Map<Long, IntakeLog> logMap = logs.stream()
                .collect(Collectors.toMap(
                        log -> log.getMedicationSchedule().getId(),
                        log -> log
                ));

        //Schedule + IntakeLog로 ui DTO 생성
        List<TodayMedicationItemResponse> items = schedules.stream()
                .map(schedule -> {

                    // 해당 스케줄의 오늘 복약 기록 조회
                    IntakeLog log = logMap.get(schedule.getId());

                    // 복약 기록이 없으면 기본 상태는 SCHEDULED
                    IntakeStatus status = log != null
                            ? log.getStatus()
                            : IntakeStatus.SCHEDULED;

                    return new TodayMedicationItemResponse(
                            schedule.getId(),
                            schedule.getMedication().getId(),
                            schedule.getMedication().getMedicationName(),

                            // 아침 / 점심 / 저녁 / 취침
                            schedule.getTimeSlot(),

                            // 실제 복용 예정 시간
                            schedule.getScheduledTime(),

                            schedule.getQuantity(),
                            schedule.getUnit(),

                            status,

                            // 복용 완료 여부
                            status == IntakeStatus.TAKEN,

                            // 복용 완료 시간
                            log != null ? log.getTakenAt() : null
                    );
                })
                .toList();

        //TimeSlot 기준 그룹핑
        Map<TimeSlot, List<TodayMedicationItemResponse>> groupedItems = items.stream()
                .collect(Collectors.groupingBy(
                        TodayMedicationItemResponse::getTimeSlot
                ));


        //최종 응답 생성
        return Arrays.stream(TimeSlot.values())
                .map(timeSlot -> new TodaySectionResponse(

                        // MORNING / LUNCH / DINNER / BEDTIME
                        timeSlot,

                        // 해당 시간대 복약 리스트
                        groupedItems.getOrDefault(timeSlot, List.of())
                ))
                .toList();
    }
}
