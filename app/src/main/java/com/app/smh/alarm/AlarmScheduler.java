package com.app.smh.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.app.smh.SettingsManager;

import java.util.Calendar;

public class AlarmScheduler {

    public static final String EXTRA_ALARM_TYPE = "extra_alarm_type";

    public static final String TYPE_MORNING = "morning";
    public static final String TYPE_LUNCH = "lunch";
    public static final String TYPE_DINNER = "dinner";
    public static final String TYPE_WAKEUP = "wakeup";
    public static final String TYPE_BEDTIME = "bedtime";

    public static void scheduleAll(Context context) {
        cancelAll(context);

        scheduleAlarm(context, TYPE_MORNING, SettingsManager.getAlarmMorningTime(context), 1001);
        scheduleAlarm(context, TYPE_LUNCH, SettingsManager.getAlarmLunchTime(context), 1002);
        scheduleAlarm(context, TYPE_DINNER, SettingsManager.getAlarmDinnerTime(context), 1003);

        if (SettingsManager.isWakeupAlarmEnabled(context)) {
            scheduleAlarm(context, TYPE_WAKEUP, SettingsManager.getAlarmWakeupTime(context), 1004);
        }

        if (SettingsManager.isBedtimeAlarmEnabled(context)) {
            scheduleAlarm(context, TYPE_BEDTIME, SettingsManager.getAlarmBedtimeTime(context), 1005);
        }
    }

    public static void cancelAll(Context context) {
        cancelAlarm(context, 1001);
        cancelAlarm(context, 1002);
        cancelAlarm(context, 1003);
        cancelAlarm(context, 1004);
        cancelAlarm(context, 1005);
    }

    private static void scheduleAlarm(Context context, String type, String time, int requestCode) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, MedicationAlarmReceiver.class);
            intent.putExtra(EXTRA_ALARM_TYPE, type);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e("AlarmScheduler", type + " 알람 등록 실패: 정확한 알람 권한 없음");
                        return;
                    }
                }
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

            Log.d("AlarmScheduler", type + " 알람 등록 완료: " + time);

        } catch (SecurityException e) {
            Log.e("AlarmScheduler", "SCHEDULE_EXACT_ALARM 권한 없음", e);
        } catch (Exception e) {
            Log.e("AlarmScheduler", "알람 등록 실패: " + type, e);
        }
    }

    private static void cancelAlarm(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, MedicationAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }
}
