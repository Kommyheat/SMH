package com.app.smh.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.app.smh.R;
import com.app.smh.SettingsManager;

public class MedicationAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medication_alarm_channel";
    // 알림 클릭 시 MainActivity에 전달할 시간대 키
    public static final String EXTRA_COMPLETE_TIME_SLOT = "extra_complete_time_slot";
    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmType = intent != null ? intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) : "unknown";

        showNotification(context, alarmType);

        if (SettingsManager.isTtsEnabled(context)) {
            String message = buildTtsMessage(alarmType);
            new TtsHelper().speak(context, message);
        }

        AlarmScheduler.scheduleAll(context);
    }

    private void showNotification(Context context, String alarmType) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "복약 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("복약 시간 알림 채널");
            manager.createNotificationChannel(channel);
        }

        // 알림 클릭 시 MainActivity로 이동 + 시간대 정보 전달
        Intent mainIntent = new Intent(context, com.app.smh.MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mainIntent.putExtra(EXTRA_COMPLETE_TIME_SLOT, alarmTypeToTimeSlot(alarmType));

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context,
                alarmType.hashCode(),
                mainIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("복약 시간 알림")
                .setContentText(buildNotificationMessage(alarmType))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent); // 알림 클릭 관련

        manager.notify(alarmType.hashCode(), builder.build());
    }

    private String buildNotificationMessage(String alarmType) {
        switch (alarmType) {
            case AlarmScheduler.TYPE_MORNING:
                return "아침 복약 시간입니다.";
            case AlarmScheduler.TYPE_LUNCH:
                return "점심 복약 시간입니다.";
            case AlarmScheduler.TYPE_DINNER:
                return "저녁 복약 시간입니다.";
            case AlarmScheduler.TYPE_WAKEUP:
                return "기상 후 복약 시간입니다.";
            case AlarmScheduler.TYPE_BEDTIME:
                return "취침 전 복약 시간입니다.";
            default:
                return "복약 시간입니다.";
        }
    }

    private String buildTtsMessage(String alarmType) {
        switch (alarmType) {
            case AlarmScheduler.TYPE_MORNING:
                return "아침 약 드실 시간입니다.";
            case AlarmScheduler.TYPE_LUNCH:
                return "점심 약 드실 시간입니다.";
            case AlarmScheduler.TYPE_DINNER:
                return "저녁 약 드실 시간입니다.";
            case AlarmScheduler.TYPE_WAKEUP:
                return "기상 후 약 드실 시간입니다.";
            case AlarmScheduler.TYPE_BEDTIME:
                return "취침 전 약 드실 시간입니다.";
            default:
                return "복약 시간입니다.";
        }
    }


    // AlarmScheduler TYPE → ScheduleMedicineItem timeSlot 변환
    private String alarmTypeToTimeSlot(String alarmType) {
        if (alarmType == null) return "";
        switch (alarmType) {
            case AlarmScheduler.TYPE_MORNING: return "아침";
            case AlarmScheduler.TYPE_LUNCH:   return "점심";
            case AlarmScheduler.TYPE_DINNER:  return "저녁";
            case AlarmScheduler.TYPE_WAKEUP:  return "기상 후";
            case AlarmScheduler.TYPE_BEDTIME: return "취침 전";
            default: return "";
        }
    }
}

