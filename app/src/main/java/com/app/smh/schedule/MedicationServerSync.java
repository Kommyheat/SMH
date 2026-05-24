package com.app.smh.schedule;

import android.content.Context;
import android.util.Log;

import com.app.smh.SettingsManager;
import com.app.smh.auth.AuthApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicationServerSync {

    private static final String TAG = "MedicationServerSync";

    public static void syncToServer(Context context, List<ScheduleMedicineItem> items) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) {
            Log.d(TAG, "로그인 상태 아님 → 서버 저장 스킵");
            return;
        }

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            Map<String, List<ScheduleMedicineItem>> groupMap = new java.util.LinkedHashMap<>();
            for (ScheduleMedicineItem item : items) {
                String key = item.getCategoryName()
                        + "_" + item.getStartDate()
                        + "_" + item.getEndDate();
                groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            for (Map.Entry<String, List<ScheduleMedicineItem>> entry : groupMap.entrySet()) {
                List<ScheduleMedicineItem> group = entry.getValue();
                ScheduleMedicineItem first = group.get(0);

                try {
                    AuthApiClient.MedicationSaveRequest medRequest =
                            new AuthApiClient.MedicationSaveRequest();
                    medRequest.userId = userId;
                    medRequest.medicationName = first.getCategoryName();
                    medRequest.ingredient = "";
                    medRequest.purpose = "";
                    medRequest.startDate = first.getStartDate();
                    medRequest.endDate = first.getEndDate();

                    Long medicationId = apiClient.createMedication(medRequest);
                    if (medicationId == null || medicationId <= 0) continue;

                    for (ScheduleMedicineItem item : group) {
                        AuthApiClient.ScheduleSaveRequest schedRequest =
                                new AuthApiClient.ScheduleSaveRequest();
                        schedRequest.userId = userId;
                        schedRequest.medicationId = medicationId;
                        schedRequest.timeSlot = toServerTimeSlot(item.getTimeSlot());
                        schedRequest.scheduledTime =
                                getScheduledTime(context, item.getTimeSlot());
                        schedRequest.quantity = 1.0;
                        schedRequest.unit = "정";
                        schedRequest.notificationEnabled = true;
                        apiClient.createSchedule(schedRequest);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "서버 저장 실패: " + first.getCategoryName(), e);
                }
            }
        }).start();
    }

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
                List<AuthApiClient.MedicationScheduleResponse> schedules =
                        apiClient.getSchedulesByUser(userId);
                List<AuthApiClient.MedicationResponse> medications =
                        apiClient.getMedicationsByUser(userId);

                if (schedules == null || schedules.isEmpty()) {
                    Log.d(TAG, "서버에 스케줄 없음");
                    if (onComplete != null) runOnMainThread(context, onComplete);
                    return;
                }

                // 로그
                Map<Long, AuthApiClient.MedicationResponse> medMap = new HashMap<>();
                if (medications != null) {
                    for (AuthApiClient.MedicationResponse med : medications) {
                        android.util.Log.d("SyncFilter", "약: " + med.medicationName
                                + " / status: " + med.status);

                        if (!"ACTIVE".equals(med.status)) {
                            android.util.Log.d("SyncFilter", "스킵: " + med.medicationName);
                            continue;
                        }
                        medMap.put(med.id, med);
                    }
                }
                android.util.Log.d("SyncFilter", "ACTIVE 약 수: " + medMap.size());

                List<ScheduleMedicineItem> items = new ArrayList<>();
                for (AuthApiClient.MedicationScheduleResponse schedule : schedules) {
                    AuthApiClient.MedicationResponse med =
                            medMap.get(schedule.medicationId);
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

                if (!items.isEmpty()) {
                    ScheduleRepository.clearAll(context);
                    ScheduleRepository.addSchedules(context, items);
                    Log.d(TAG, "스케줄 동기화 완료: " + items.size() + "개");
                }

                // onComplete를 함께 전달
                restoreCompletedStatus(context, apiClient, userId, items, onComplete);

            } catch (Exception e) {
                Log.e(TAG, "syncFromServer 실패", e);
                if (onComplete != null) runOnMainThread(context, onComplete);
            }
        }).start();
    }

    // 파라미터 5개 버전
    private static void restoreCompletedStatus(Context context,
                                               AuthApiClient apiClient,
                                               long userId,
                                               List<ScheduleMedicineItem> items,
                                               Runnable onComplete) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentYear = cal.get(java.util.Calendar.YEAR);
            int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;

            List<AuthApiClient.IntakeLogResponse> allLogs = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                int year = currentYear;
                int month = currentMonth - i;
                if (month <= 0) {
                    month += 12;
                    year -= 1;
                }
                try {
                    List<AuthApiClient.IntakeLogResponse> monthLogs =
                            apiClient.getMyMonthlyLogs(userId, year, month);
                    if (monthLogs != null) allLogs.addAll(monthLogs);
                } catch (Exception e) {
                    Log.e(TAG, "월별 기록 조회 실패: " + year + "-" + month, e);
                }
            }

            Log.d(TAG, "intake_logs 조회 완료: " + allLogs.size() + "개");

            if (!allLogs.isEmpty()) {
                ArrayList<ScheduleMedicineItem> all =
                        ScheduleRepository.getAllSchedules(context);

                for (AuthApiClient.IntakeLogResponse log : allLogs) {
                    if (!"TAKEN".equals(log.status)) continue;
                    if (log.date == null || log.medicationName == null) continue;

                    String koreanTimeSlot = toKoreanTimeSlot(log.timeSlot);

                    for (ScheduleMedicineItem item : all) {
                        if (item.getCategoryName().equals(log.medicationName)
                                && item.getTimeSlot().equals(koreanTimeSlot)) {
                            item.setCompletedOn(log.date, true);
                            Log.d(TAG, "완료 상태 복원: "
                                    + log.medicationName + " / " + log.date);
                            break;
                        }
                    }
                }

                ScheduleRepository.saveAllSchedules(context, all);
                Log.d(TAG, "완료 상태 복원 저장 완료");
            }

        } catch (Exception e) {
            Log.e(TAG, "완료 상태 복원 실패", e);
        } finally {
            // onComplete 반드시 호출
            if (onComplete != null) runOnMainThread(context, onComplete);
        }
    }

    public static void deleteMedication(Context context,
                                        ScheduleMedicineItem item,
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
                List<AuthApiClient.MedicationResponse> medications =
                        apiClient.getMedicationsByUser(userId);
                if (medications != null) {
                    for (AuthApiClient.MedicationResponse med : medications) {
                        if (med.medicationName.equals(item.getCategoryName())) {
                            apiClient.changeMedicationStatus(med.id, userId, "STOPPED");
                            Log.d(TAG, "서버 약 STOPPED: medicationId=" + med.id);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "서버 약 상태 변경 실패 (로컬은 삭제됨)", e);
            } finally {
                ScheduleRepository.removeSchedule(context, item);
                if (onComplete != null) runOnMainThread(context, onComplete);
            }
        }).start();
    }

    public static void updateScheduleTimeBySlot(Context context,
                                                String serverTimeSlot,
                                                String newTime) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) return;

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                List<AuthApiClient.MedicationScheduleResponse> schedules =
                        apiClient.getSchedulesByUser(userId);
                if (schedules == null) return;

                for (AuthApiClient.MedicationScheduleResponse schedule : schedules) {
                    if (serverTimeSlot.equals(schedule.timeSlot)) {
                        apiClient.updateScheduleTime(schedule.id, userId, newTime + ":00");
                        Log.d(TAG, "스케줄 시간 업데이트: scheduleId="
                                + schedule.id + " / " + newTime);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "스케줄 시간 업데이트 실패", e);
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
     * 여러 아이템 일괄 삭제
     * 서버 약 목록 1번만 조회 후 매칭
     */
    public static void deleteMedicationBatch(Context context,
                                             ArrayList<ScheduleMedicineItem> items,
                                             Runnable onComplete) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) {
            for (ScheduleMedicineItem item : items) {
                ScheduleRepository.removeSchedule(context, item);
            }
            if (onComplete != null) runOnMainThread(context, onComplete);
            return;
        }

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                // 서버 약 목록 1번만 조회
                List<AuthApiClient.MedicationResponse> medications =
                        apiClient.getMedicationsByUser(userId);

                for (ScheduleMedicineItem item : items) {
                    try {
                        if (medications != null) {
                            for (AuthApiClient.MedicationResponse med : medications) {
                                // 로그
                                android.util.Log.d("DeleteBatch",
                                        "서버약: " + med.medicationName
                                                + " startDate: " + med.startDate
                                                + " endDate: " + med.endDate);
                                android.util.Log.d("DeleteBatch",
                                        "삭제대상: " + item.getCategoryName()
                                                + " startDate: " + item.getStartDate()
                                                + " endDate: " + item.getEndDate());

                                if (med.medicationName.equals(item.getCategoryName())
                                        && med.startDate.equals(item.getStartDate())
                                        && med.endDate.equals(item.getEndDate())) {

                                    String serverTimeSlot = toServerTimeSlot(item.getTimeSlot());
                                    apiClient.deleteMedicationByTimeSlot(
                                            med.id, userId, serverTimeSlot);
                                    Log.d(TAG, "서버 삭제 완료: " + item.getCategoryName()
                                            + " / " + serverTimeSlot);
                                    break;
                                }
                            }
                        }
                        // 로컬 삭제
                        ScheduleRepository.removeSchedule(context, item);

                    } catch (Exception e) {
                        Log.e(TAG, "개별 삭제 실패: " + item.getCategoryName(), e);
                        ScheduleRepository.removeSchedule(context, item);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "일괄 삭제 실패", e);
                // 실패해도 로컬은 삭제
                for (ScheduleMedicineItem item : items) {
                    ScheduleRepository.removeSchedule(context, item);
                }
            } finally {
                if (onComplete != null) runOnMainThread(context, onComplete);
            }
        }).start();
    }
}
