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
import com.app.smh.auth.AuthApiClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MissedAlertReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "missed_alert_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 미복용 알림 설정 꺼져있으면 무시
        if (!SettingsManager.isMissedAlertEnabled(context)) return;

        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) return;

        // 백그라운드에서 오늘 복약 기록 확인
        new Thread(() -> {
            try {
                AuthApiClient apiClient = new AuthApiClient();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().getTime());

                // 오늘 복약 기록 조회
                List<AuthApiClient.IntakeLogResponse> logs =
                        apiClient.getMyMonthlyLogs(userId,
                                Calendar.getInstance().get(Calendar.YEAR),
                                Calendar.getInstance().get(Calendar.MONTH) + 1);

                if (logs == null) return;

                // 오늘 SCHEDULED(미완료) 항목 확인
                boolean hasMissed = false;
                for (AuthApiClient.IntakeLogResponse log : logs) {
                    if (today.equals(log.date) && "SCHEDULED".equals(log.status)) {
                        hasMissed = true;
                        break;
                    }
                }

                if (hasMissed) {
                    // 보호자에게 알림 전송
                    sendNotificationToCaregiver(context);
                }

            } catch (Exception e) {
                android.util.Log.e("MissedAlert", "체크 실패", e);
            }
        }).start();
    }

    private void sendNotificationToCaregiver(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "미복용 알림",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("미복용 알림")
                        .setContentText("피보호자가 오늘 복약을 완료하지 않았습니다.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        manager.notify(2001, builder.build());
    }
}
