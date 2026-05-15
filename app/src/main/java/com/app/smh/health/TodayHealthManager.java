package com.app.smh.health;

import android.content.Context;

import com.app.smh.SettingsManager;
import com.app.smh.schedule.ScheduleMedicineItem;
import com.app.smh.schedule.ScheduleRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TodayHealthManager {

    // 가장 가까운 미완료 복용 일정 정보를 담는 데이터 클래스
    public static class UpcomingMedicineInfo {
        public String timeSlot;              // 아침 / 점심 / 저녁
        public String alarmTime;             // 24시간제 저장값 "HH:mm"
        public ArrayList<String> medicineNames; // 해당 시간대 미완료 약 이름 목록

        public UpcomingMedicineInfo() {
            medicineNames = new ArrayList<>();
        }
    }

    /**
     * 현재 시간 기준 가장 가까운 미완료 복용 일정 반환
     *
     * 로직:
     * 1. 오늘 날짜 기준으로 ScheduleRepository에서 스케줄 조회
     * 2. SettingsManager에서 아침/점심/저녁 알림 시간 가져오기
     * 3. 각 시간대별 미완료 약이 있는지 확인
     * 4. 현재 시각과 각 시간대의 차이를 계산 (지난 시간대는 +24시간)
     * 5. 가장 차이가 작은 시간대 반환
     */
    public static UpcomingMedicineInfo getUpcomingMedicine(Context context) {
        // 오늘 날짜 문자열
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // 오늘 등록된 전체 스케줄
        ArrayList<ScheduleMedicineItem> todaySchedules =
                ScheduleRepository.getSchedulesByDate(context, today);

        if (todaySchedules == null || todaySchedules.isEmpty()) return null;

        // 현재 시각 (분 단위)
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        // 아침/점심/저녁 시간대 + SettingsManager에서 설정된 알림 시간
        String[] slots = {"아침", "점심", "저녁"};
        String[] times = {
                SettingsManager.getAlarmMorningTime(context),  // 예: "08:00"
                SettingsManager.getAlarmLunchTime(context),    // 예: "13:00"
                SettingsManager.getAlarmDinnerTime(context)    // 예: "19:00"
        };

        UpcomingMedicineInfo closest = null;
        int minDiff = Integer.MAX_VALUE;

        for (int i = 0; i < slots.length; i++) {
            String slot = slots[i];
            String time = times[i];

            // 해당 시간대의 미완료 약 이름 수집
            ArrayList<String> names = new ArrayList<>();
            for (ScheduleMedicineItem item : todaySchedules) {
                if (item == null) continue;
                if (slot.equals(item.getTimeSlot()) && !item.isCompleted()) {
                    names.add(item.getCategoryName());
                }
            }

            // 미완료 약이 없으면 스킵
            if (names.isEmpty()) continue;

            // 알림 시간 → 분 단위 변환
            int slotMinutes = parseTimeToMinutes(time);
            if (slotMinutes < 0) continue;

            // 현재 시각과의 차이 (이미 지난 시간대는 내일 기준으로 +24시간)
            int diff = slotMinutes - nowMinutes;
            if (diff < 0) diff += 24 * 60;

            if (diff < minDiff) {
                minDiff = diff;
                closest = new UpcomingMedicineInfo();
                closest.timeSlot = slot;
                closest.alarmTime = time;
                closest.medicineNames = names;
            }
        }

        return closest;
    }

    /**
     * 복용 시간까지 남은 시간에 따라 메시지 생성
     *
     * 30분 이내  → "곧 복용 시간이에요!"
     * 1시간 이내 → "약 N분 후 복용 시간이에요"
     * 그 외      → "오전/오후 HH:mm 복용 시간이에요"
     */
    public static String buildUpcomingMessage(UpcomingMedicineInfo info) {
        if (info == null) return null;

        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int slotMinutes = parseTimeToMinutes(info.alarmTime);

        int diff = slotMinutes - nowMinutes;
        if (diff < 0) diff += 24 * 60;

        // 남은 시간 문자열 결정
        String timeDesc;
        if (diff <= 30) {
            timeDesc = "곧 복용 시간이에요!";
        } else if (diff <= 60) {
            timeDesc = "약 " + diff + "분 후 복용 시간이에요";
        } else {
            // AlarmSettingsActivity의 to12HourDisplay()와 동일한 변환 로직
            timeDesc = to12HourDisplay(info.alarmTime) + " 복용 시간이에요";
        }

        // 약 이름 목록 조합
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < info.medicineNames.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(info.medicineNames.get(i));
        }

        return info.timeSlot + " 약 복용 시간\n"
                + timeDesc + "\n"
                + "【 " + sb.toString() + " 】";
    }


    private static int parseTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * AlarmSettingsActivity와 동일한 24시간제 → 12시간제 변환
     * "08:00" → "오전 08:00"
     * "13:00" → "오후 01:00"
     */
    public static String to12HourDisplay(String time24) {
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String ampm = (hour < 12) ? "오전" : "오후";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format("%s %02d:%02d", ampm, hour12, minute);
        } catch (Exception e) {
            return time24;
        }
    }
}
