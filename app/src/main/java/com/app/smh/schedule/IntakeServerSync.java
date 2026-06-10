package com.app.smh.schedule;

import android.content.Context;
import android.util.Log;

import com.app.smh.SettingsManager;
import com.app.smh.auth.AuthApiClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IntakeServerSync {

    private static final String TAG = "IntakeServerSync";

    /**
     * 완료 버튼 클릭 시 서버 intake_logs에 TAKEN 저장
     */
    public static void syncTaken(Context context, String categoryName,
                                 String timeSlot, String date) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) {
            Log.d(TAG, "로그인 상태 아님 → 스킵");
            return;
        }

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                // 1. 스케줄 목록 조회
                List<AuthApiClient.MedicationScheduleResponse> schedules =
                        apiClient.getSchedulesByUser(userId);

                // 2. categoryName + timeSlot 매칭
                long scheduleId = findScheduleId(schedules, categoryName, timeSlot);
                if (scheduleId <= 0) {
                    Log.e(TAG, "스케줄 없음: " + categoryName + " / " + timeSlot);
                    return;
                }

                // 3. intake_logs에 TAKEN 저장
                AuthApiClient.IntakeTakeRequest request = new AuthApiClient.IntakeTakeRequest();
                request.userId = userId;
                request.date = date;
                request.memo = "";

                apiClient.takeMedication(scheduleId, request);
                Log.d(TAG, "복약 완료 저장: scheduleId=" + scheduleId
                        + " / " + categoryName + " / " + date);

            } catch (Exception e) {
                Log.e(TAG, "복약 완료 서버 저장 실패 (로컬은 저장됨)", e);
            }
        }).start();
    }

    /**
     * 미완료로 되돌릴 때 서버 intake_logs 취소
     */
    public static void syncCanceled(Context context, String categoryName,
                                    String timeSlot, String date) {
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) return;

        AuthApiClient apiClient = new AuthApiClient();

        new Thread(() -> {
            try {
                List<AuthApiClient.MedicationScheduleResponse> schedules =
                        apiClient.getSchedulesByUser(userId);

                long scheduleId = findScheduleId(schedules, categoryName, timeSlot);
                if (scheduleId <= 0) return;

                AuthApiClient.IntakeTakeRequest request = new AuthApiClient.IntakeTakeRequest();
                request.userId = userId;
                request.date = date;
                request.memo = "";

                apiClient.cancelTake(scheduleId, request);
                Log.d(TAG, "복약 취소 저장: scheduleId=" + scheduleId
                        + " / " + categoryName + " / " + date);

            } catch (Exception e) {
                Log.e(TAG, "복약 취소 서버 저장 실패 (로컬은 저장됨)", e);
            }
        }).start();
    }

    /**
     * 스케줄 목록에서 categoryName + timeSlot 매칭으로 scheduleId 찾기
     */
    private static long findScheduleId(
            List<AuthApiClient.MedicationScheduleResponse> schedules,
            String categoryName, String timeSlot) {

        String serverTimeSlot = toServerTimeSlot(timeSlot);

        for (AuthApiClient.MedicationScheduleResponse s : schedules) {
            if (s.medicationName.equals(categoryName)
                    && s.timeSlot.equals(serverTimeSlot)) {
                return s.id;
            }
        }
        return -1;
    }

    /**
     * 안드로이드 timeSlot → 서버 TimeSlot 변환
     */
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
}
