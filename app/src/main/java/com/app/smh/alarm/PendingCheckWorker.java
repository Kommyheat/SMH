package com.app.smh.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.app.smh.GuardianLinkActivity;
import com.app.smh.R;
import com.app.smh.SettingsManager;
import com.app.smh.auth.AuthApiClient;

public class PendingCheckWorker extends Worker {

    private static final String CHANNEL_ID = "pending_alert_channel";

    public PendingCheckWorker(@NonNull Context context,
                              @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        long userId = SettingsManager.getLoginUserId(context);
        if (userId <= 0) return Result.success();

        try {
            AuthApiClient apiClient = new AuthApiClient();
            AuthApiClient.CareLinkStatusResponse status =
                    apiClient.getCareLinkStatus(userId);

            // PENDING 상태이고 내가 받은 요청일 때만 알림
            if (status != null
                    && "PENDING".equals(status.status)
                    && status.caregiverId != userId) {

                String senderName = status.caregiverName != null
                        ? status.caregiverName : "누군가";
                sendNotification(context, senderName);
            }
        } catch (Exception e) {
            // 실패해도 앱 동작에 영향 없음
        }

        return Result.success();
    }

    private void sendNotification(Context context, String senderName) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "연동 요청 알림",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        // 클릭 시 GuardianLinkActivity 이동
        Intent intent = new Intent(context, GuardianLinkActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("연동 요청 도착")
                        .setContentText(senderName + "님이 연동을 요청했어요")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        manager.notify(3001, builder.build());
    }
}
