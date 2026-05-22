package com.app.smh.schedule;

import android.content.Context;
import android.util.Log;

import com.app.smh.SettingsManager;
import com.app.smh.auth.AuthApiClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MedicationServerSync {

    private static final String TAG = "MedicationServerSync";

    /**
     * 로컬 → 서버 저장
     * 약 등록 시 호출 (스캔/수기 등록)
     */
    public static void syncToServer(Context context, List<ScheduleMedicineItem> items) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) return;

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                // categoryName + startDate + endDate 기준으로 그룹핑
                Map<String, List<ScheduleMedicineItem>> groupMap = new LinkedHashMap<>();
                for (ScheduleMedicineItem item : items) {
                    String key = item.getCategoryName()
                            + "_" + item.getStartDate()
                            + "_" + item.getEndDate();
                    groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                }

                for (Map.Entry<String, List<ScheduleMedicineItem>> entry : groupMap.entrySet()) {
                    List<ScheduleMedicineItem> group = entry.getValue();
                    ScheduleMedicineItem first = group.get(0);

                    // 약 1개만 등록
                    AuthApiClient.MedicationSaveRequest medRequest =
                            new AuthApiClient.MedicationSaveRequest();
                    medRequest.userId = userId;
                    medRequest.medicationName = first.getCategoryName();
                    medRequest.ingredient = "";
                    medRequest.purpose = "";
                    medRequest.startDate = first.getStartDate();
                    medRequest.endDate = first.getEndDate();

                    Long medicationId = apiClient.createMedication(medRequest);
                    Log.d(TAG, "약 등록 완료: medicationId=" + medicationId
                            + " / " + first.getCategoryName());

                    if (medicationId == null || medicationId <= 0) continue;

                    // 시간대별 스케줄 각각 등록
                    for (ScheduleMedicineItem item : group) {
                        AuthApiClient.ScheduleSaveRequest schedRequest =
                                new AuthApiClient.ScheduleSaveRequest();
                        schedRequest.userId = userId;
                        schedRequest.medicationId = medicationId;
                        schedRequest.timeSlot = toServerTimeSlot(item.getTimeSlot());
                        schedRequest.scheduledTime = getScheduledTime(context, item.getTimeSlot());
                        schedRequest.quantity = 1.0;
                        schedRequest.unit = "정";
                        schedRequest.notificationEnabled = true;

                        Long scheduleId = apiClient.createSchedule(schedRequest);
                        Log.d(TAG, "스케줄 등록: scheduleId=" + scheduleId
                                + " / timeSlot=" + schedRequest.timeSlot);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 저장 실패", e);
            }
        }).start();
    }

    private static String getScheduledTime(Context context, String timeSlot) {
        if (timeSlot == null) return "08:00:00";
        switch (timeSlot) {
            case "아침": {
                String t = SettingsManager.getAlarmMorningTime(context);
                return t != null ? t + ":00" : "08:00:00";
            }
            case "점심": {
                String t = SettingsManager.getAlarmLunchTime(context);
                return t != null ? t + ":00" : "12:00:00";
            }
            case "저녁": {
                String t = SettingsManager.getAlarmDinnerTime(context);
                return t != null ? t + ":00" : "18:00:00";
            }
            case "취침 전": {
                String t = SettingsManager.getAlarmBedtimeTime(context);
                return t != null ? t + ":00" : "21:30:00";
            }
            case "기상 후": {
                String t = SettingsManager.getAlarmWakeupTime(context);
                return t != null ? t + ":00" : "07:00:00";
            }
            default: return "08:00:00";
        }
    }

    /**
     * 서버 → 로컬 동기화
     * 로그인 시 호출 (앱 재설치 후 데이터 복원)
     */
    public static void syncFromServer(Context context, Runnable onComplete) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) {
            Log.d(TAG, "로그인 상태 아님 → 동기화 스킵");
            if (onComplete != null) runOnMainThread(context, onComplete);
            return;
        }

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                // 1. 서버에서 스케줄 목록 조회
                List<AuthApiClient.MedicationScheduleResponse> schedules =
                        apiClient.getSchedulesByUser(userId);

                // 2. 서버에서 약 목록 조회 (기간 정보 필요)
                List<AuthApiClient.MedicationResponse> medications =
                        apiClient.getMedicationsByUser(userId);

                if (schedules == null || schedules.isEmpty()) {
                    Log.d(TAG, "서버에 스케줄 없음");
                    if (onComplete != null) runOnMainThread(context, onComplete);
                    return;
                }

                // 3. Map으로 변환 (medicationId → MedicationResponse)
                Map<Long, AuthApiClient.MedicationResponse> medMap = new HashMap<>();
                if (medications != null) {
                    for (AuthApiClient.MedicationResponse med : medications) {
                        medMap.put(med.id, med);
                    }
                }

                // 4. ScheduleMedicineItem 리스트 생성
                List<ScheduleMedicineItem> items = new ArrayList<>();
                for (AuthApiClient.MedicationScheduleResponse schedule : schedules) {
                    AuthApiClient.MedicationResponse med = medMap.get(schedule.medicationId);
                    if (med == null) continue;

                    ScheduleMedicineItem item = new ScheduleMedicineItem();
                    item.setCategoryName(schedule.medicationName);
                    item.setTimeSlot(toKoreanTimeSlot(schedule.timeSlot));
                    item.setStartDate(med.startDate);
                    item.setEndDate(med.endDate);
                    item.setCompleted(false);
                    item.setDrugNames(new ArrayList<>());
                    items.add(item);
                }

                // 5. 로컬 저장
                if (!items.isEmpty()) {
                    ScheduleRepository.clearAll(context);
                    ScheduleRepository.addSchedules(context, items);
                    Log.d(TAG, "서버 → 로컬 동기화 완료: " + items.size() + "개");
                }

                if (onComplete != null) runOnMainThread(context, onComplete);

            } catch (Exception e) {
                Log.e(TAG, "서버 → 로컬 동기화 실패", e);
                if (onComplete != null) runOnMainThread(context, onComplete);
            }
        }).start();
    }

    // 안드로이드 timeSlot → 서버 TimeSlot 변환
    private static String toServerTimeSlot(String timeSlot) {
        if (timeSlot == null) return "MORNING";
        switch (timeSlot) {
            case "아침":    return "MORNING";
            case "점심":    return "LUNCH";
            case "저녁":    return "DINNER";
            case "취침 전":  return "BEDTIME";
            case "기상 후":  return "WAKEUP";
            default:        return "MORNING";
        }
    }

    // 서버 TimeSlot → 안드로이드 timeSlot 변환
    private static String toKoreanTimeSlot(String timeSlot) {
        if (timeSlot == null) return "아침";
        switch (timeSlot) {
            case "MORNING": return "아침";
            case "LUNCH":   return "점심";
            case "DINNER":  return "저녁";
            case "BEDTIME": return "취침 전";
            case "WAKEUP":  return "기상 후";
            default:        return "아침";
        }
    }

    private static void runOnMainThread(Context context, Runnable runnable) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(runnable);
    }
    /**
     * 약 삭제 (서버에서 STOPPED 상태로 변경 + 로컬에서 제거)
     */
    public static void deleteMedication(Context context, ScheduleMedicineItem item,
                                        Runnable onComplete) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) {
            ScheduleRepository.removeSchedule(context, item);
            if (onComplete != null) runOnMainThread(context, onComplete);
            return;
        }

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                // 서버에서 해당 약 찾기
                List<AuthApiClient.MedicationResponse> medications =
                        apiClient.getMedicationsByUser(userId);

                if (medications != null) {
                    for (AuthApiClient.MedicationResponse med : medications) {
                        if (med.medicationName.equals(item.getCategoryName())) {
                            // 서버에서 STOPPED 상태로 변경
                            apiClient.changeMedicationStatus(med.id, userId, "STOPPED");
                            Log.d(TAG, "서버 약 STOPPED: medicationId=" + med.id);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 약 상태 변경 실패 (로컬은 삭제됨)", e);
            } finally {
                // 로컬에서는 무조건 삭제
                ScheduleRepository.removeSchedule(context, item);
                if (onComplete != null) runOnMainThread(context, onComplete);
            }
        }).start();
    }

    /**
     * 알림 설정 변경 시 서버 스케줄 시간 자동 업데이트
     * timeSlot: "MORNING", "LUNCH", "DINNER", "BEDTIME"
     */
    public static void updateScheduleTimeBySlot(Context context,
                                                String serverTimeSlot,
                                                String newTime) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) return;

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                // 해당 userId의 스케줄 목록 조회
                List<AuthApiClient.MedicationScheduleResponse> schedules =
                        apiClient.getSchedulesByUser(userId);

                if (schedules == null) return;

                // timeSlot이 일치하는 스케줄 모두 업데이트
                for (AuthApiClient.MedicationScheduleResponse schedule : schedules) {
                    if (serverTimeSlot.equals(schedule.timeSlot)) {
                        apiClient.updateScheduleTime(
                                schedule.id,
                                userId,
                                newTime + ":00"
                        );
                        Log.d(TAG, "스케줄 시간 업데이트: scheduleId="
                                + schedule.id + " / " + newTime);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "스케줄 시간 업데이트 실패", e);
            }
        }).start();
    }
}
