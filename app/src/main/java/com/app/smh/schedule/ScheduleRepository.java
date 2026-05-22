package com.app.smh.schedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.app.smh.scan.DrugInfoApiManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleRepository {

    private static final String PREF_NAME = "schedule_prefs";
    private static final String KEY_SCHEDULES = "schedules";

    // 스케줄 추가 (로컬 저장)
    public static void addSchedules(Context context, List<ScheduleMedicineItem> items) {
        if (items == null || items.isEmpty()) return;
        ArrayList<ScheduleMedicineItem> all = getAllSchedules(context);
        all.addAll(items);
        saveLocal(context, all);
    }

    public static void addSchedule(Context context, ScheduleMedicineItem item) {
        if (item == null) return;
        ArrayList<ScheduleMedicineItem> all = getAllSchedules(context);
        all.add(item);
        saveLocal(context, all);
    }

    /**
     * 달력에서 재조회한 상세정보를 로컬에 업데이트
     */
    public static void updateDrugDetails(Context context,
                                         ScheduleMedicineItem targetItem) {
        ArrayList<ScheduleMedicineItem> all = getAllSchedules(context);
        for (ScheduleMedicineItem item : all) {
            if (item != null &&
                    item.getCategoryName().equals(targetItem.getCategoryName()) &&
                    item.getStartDate().equals(targetItem.getStartDate()) &&
                    item.getTimeSlot().equals(targetItem.getTimeSlot())) {
                item.setDrugDetails(targetItem.getDrugDetails());
                break;
            }
        }
        saveLocal(context, all);
    }

    // 로컬에서 전체 불러오기
    public static ArrayList<ScheduleMedicineItem> getAllSchedules(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SCHEDULES, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<ArrayList<ScheduleMedicineItem>>() {}.getType();
        ArrayList<ScheduleMedicineItem> result = new Gson().fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }

    public static ArrayList<ScheduleMedicineItem> getSchedulesByDate(Context context, String date) {
        ArrayList<ScheduleMedicineItem> result = new ArrayList<>();
        for (ScheduleMedicineItem item : getAllSchedules(context)) {
            if (item != null && item.isActiveOn(date)) {
                result.add(item);
            }
        }
        return result;
    }

    // 로컬 스케줄 전체 삭제 (로그아웃 시 호출)
    public static void clearAll(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_SCHEDULES)
                .apply();
    }

    // 서버 동기화 (현재는 서버 API 미구현으로 로컬만 사용)
    public static void syncFromServer(Context context, Runnable onComplete) {
        Log.d("ScheduleRepository", "syncFromServer: 서버 API 미구현 상태, 로컬 데이터 사용");
        if (onComplete != null) {
            onComplete.run();
        }
    }

    // 스케줄 삭제
    public static void removeSchedule(Context context, ScheduleMedicineItem targetItem) {
        ArrayList<ScheduleMedicineItem> all = getAllSchedules(context);
        all.removeIf(item ->
                item != null &&
                        item.getCategoryName().equals(targetItem.getCategoryName()) &&
                        item.getStartDate().equals(targetItem.getStartDate()) &&
                        item.getTimeSlot().equals(targetItem.getTimeSlot())
        );
        saveLocal(context, all);
    }

    /**
     * 특정 날짜의 완료 상태만 업데이트
     * 다른 날짜에는 영향 없음
     * 5/9에 완료 눌러도 5/10~5/12는 미완료 유지
     */
    public static void updateCompletedForDate(Context context,
                                              ScheduleMedicineItem targetItem,
                                              String date) {
        ArrayList<ScheduleMedicineItem> all = getAllSchedules(context);
        for (ScheduleMedicineItem item : all) {
            if (item != null &&
                    item.getCategoryName().equals(targetItem.getCategoryName()) &&
                    item.getStartDate().equals(targetItem.getStartDate()) &&
                    item.getTimeSlot().equals(targetItem.getTimeSlot())) {
                // 해당 날짜의 완료 상태만 변경
                item.setCompletedOn(date, targetItem.isCompletedOn(date));
                break;
            }
        }
        saveLocal(context, all);
    }

    /**
     * 기존 updateCompleted 유지 (하위 호환)
     * 오늘 날짜 기준으로 updateCompletedForDate 호출
     */
    public static void updateCompleted(Context context, ScheduleMedicineItem targetItem) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        updateCompletedForDate(context, targetItem, today);
    }

    // 로컬 저장 헬퍼
    private static void saveLocal(Context context, ArrayList<ScheduleMedicineItem> items) {
        String json = new Gson().toJson(items);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SCHEDULES, json)
                .apply();
    }

    public static void updateScheduleId(Context context,
                                        ScheduleMedicineItem targetItem,
                                        Long scheduleId) {
        if (targetItem == null || scheduleId == null) return;

        ArrayList<ScheduleMedicineItem> all = getAllSchedules(context);
        boolean updated = false;

        for (ScheduleMedicineItem item : all) {
            if (item == null) continue;

            boolean matched =
                    java.util.Objects.equals(item.getCategoryName(), targetItem.getCategoryName()) &&
                            java.util.Objects.equals(item.getStartDate(), targetItem.getStartDate()) &&
                            java.util.Objects.equals(item.getEndDate(), targetItem.getEndDate()) &&
                            java.util.Objects.equals(item.getTimeSlot(), targetItem.getTimeSlot());

            Log.d("ScheduleRepository", "비교중 item="
                    + item.getCategoryName()
                    + ", startDate=" + item.getStartDate()
                    + ", endDate=" + item.getEndDate()
                    + ", timeSlot=" + item.getTimeSlot()
                    + ", matched=" + matched);

            if (matched) {
                item.setScheduleId(scheduleId);
                updated = true;
                Log.d("ScheduleRepository", "scheduleId 반영 성공: " + scheduleId);
                break;
            }
        }

        if (updated) {
            saveLocal(context, all);
            Log.d("ScheduleRepository", "saveLocal 완료");
        } else {
            Log.d("ScheduleRepository", "일치 항목 없음 → scheduleId 저장 실패");
        }
    }
    // 전체 스케줄 저장 (완료 상태 포함)
    public static void saveAllSchedules(Context context,
                                        ArrayList<ScheduleMedicineItem> items) {
        saveLocal(context, items);
    }

}
