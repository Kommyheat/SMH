package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.CalenderDayStatus;
import org.example.domain.IntakeLog;
import org.example.domain.IntakeStatus;
import org.example.domain.MedicationSchedule;
import org.example.dto.CalendarDayResponse;
import org.example.dto.CalendarItemResponse;
import org.example.dto.CalendarMonthResponse;
import org.example.repository.IntakeLogRepository;
import org.example.repository.MedicationScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationCalendarService {

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final IntakeLogRepository intakeLogRepository;

    //특정 날짜의 복약 상세 리스트 조회
    //캘린더 화면에서 특정 날짜를 클릭했을 때
    public List<CalendarItemResponse> getDailyCalendar(Long userId, LocalDate date) {

        // 해당 날짜에 활성화된 복약 스케줄 조회
        List<MedicationSchedule> schedules =
                medicationScheduleRepository.findActiveSchedulesByUserIdAndDate(userId, date);

        // 해당 날짜의 복약 로그 조회
        List<IntakeLog> logs =
                intakeLogRepository.findByMedicationSchedule_Medication_User_IdAndDate(userId, date);

        //scheduleId 기준으로 IntakeLog를 빠르게 찾기 위한 Map
        Map<Long, IntakeLog> logMap = logs.stream()
                .collect(Collectors.toMap(
                        log -> log.getMedicationSchedule().getId(),
                        log -> log
                ));

        return schedules.stream()
                .map(schedule -> {
                    IntakeLog log = logMap.get(schedule.getId());

                    //복약 로그가 있으면 로그의 상태 사용
                    //복약 로그가 없으면 아직 복용 전이므로 SCHEDULED 처리
                    IntakeStatus status = log != null
                            ? log.getStatus()
                            : IntakeStatus.SCHEDULED;

                    return new CalendarItemResponse(
                            schedule.getId(),
                            schedule.getMedication().getId(),
                            schedule.getMedication().getMedicationName(),
                            schedule.getTimeSlot(),
                            schedule.getScheduledTime(),
                            schedule.getQuantity(),
                            schedule.getUnit(),
                            status,
                            log != null ? log.getTakenAt() : null,
                            log != null ? log.getMemo() : null
                    );
                })
                .collect(Collectors.toList());
    }

    //월간 캘린더 조회
    //캘린더 화면의 월간 달력
    public CalendarMonthResponse getMonthlyCalendar(Long userId, int year, int month) {

        YearMonth yearMonth = YearMonth.of(year, month);

        // 해당 월의 시작일과 종료일
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarDayResponse> dayResponses = new ArrayList<>();

        //해당 월의 모든 날짜를 하루씩 순회하면tj 날짜별 복약 상태를 계산
        for (LocalDate date = startDate;
             !date.isAfter(endDate);
             date = date.plusDays(1)) {

            // 해당 날짜에 복용해야 하는 활성 스케줄 조회
            List<MedicationSchedule> schedules =
                    medicationScheduleRepository.findActiveSchedulesByUserIdAndDate(userId, date);

            // 해당 날짜의 복약 로그 조회
            List<IntakeLog> logs =
                    intakeLogRepository.findByMedicationSchedule_Medication_User_IdAndDate(userId, date);

            int totalCount = schedules.size();

            // 완료된 복약 개수
            int takenCount = (int) logs.stream()
                    .filter(log -> log.getStatus() == IntakeStatus.TAKEN)
                    .count();

            // MISSED 처리된 복약 개수
            int missedCount = (int) logs.stream()
                    .filter(log -> log.getStatus() == IntakeStatus.MISSED)
                    .count();

            CalenderDayStatus status = calculateDayStatus(
                    date,
                    totalCount,
                    takenCount,
                    missedCount
            );

            dayResponses.add(
                    new CalendarDayResponse(
                            date,
                            status,
                            totalCount,
                            takenCount,
                            missedCount
                    )
            );
        }

        return new CalendarMonthResponse(
                year,
                month,
                dayResponses
        );
    }

    //날짜별 캘린더 상태 계산 메서드
    private CalenderDayStatus calculateDayStatus(
            LocalDate date,
            int totalCount,
            int takenCount,
            int missedCount
    ) {

        // 복약 일정이 없는 날
        if (totalCount == 0) {
            return CalenderDayStatus.EMPTY;
        }

        // 모든 복약을 완료한 날
        if (takenCount == totalCount) {
            return CalenderDayStatus.COMPLETED;
        }

        // 명시적으로 MISSED 상태가 하나라도 있으면 미복용 표시
        if (missedCount > 0) {
            return CalenderDayStatus.MISSED;
        }

        // 과거 날짜인데 완료되지 않은 복약이 있으면 미복용으로 판단
        if (date.isBefore(LocalDate.now()) && takenCount < totalCount) {
            return CalenderDayStatus.MISSED;
        }

        // 일부만 복용 완료한 날
        if (takenCount > 0) {
            return CalenderDayStatus.PARTIAL;
        }

        // 복약 일정은 있지만 아직 복용 전인 날
        return CalenderDayStatus.SCHEDULED;
    }
}
