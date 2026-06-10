package com.app.smh;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.app.smh.schedule.MedicationServerSync;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (SettingsManager.isDarkModeEnabled(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashLogo = findViewById(R.id.iv_splash_logo);
        if (splashLogo == null || findViewById(R.id.tv_splash_title) == null) {
            goToNext();
            return;
        }

        if (splashLogo instanceof ImageView) {
            applyDarkModeLogoOverlay((ImageView) splashLogo);
        }

        splashLogo.postDelayed(this::goToNext, 2500);
    }

    private void applyDarkModeLogoOverlay(ImageView logoView) {
        int nightMode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            logoView.setColorFilter(
                    ContextCompat.getColor(this, R.color.logo_dark_overlay),
                    PorterDuff.Mode.SRC_ATOP
            );
        } else {
            logoView.clearColorFilter();
        }
    }

    private void goToNext() {
        schedulePendingCheck();
        if (SettingsManager.isLoggedIn(this)) {
            MedicationServerSync.syncFromServer(this, () ->
                    runOnUiThread(() -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
            );
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void schedulePendingCheck() {
        androidx.work.PeriodicWorkRequest workRequest =
                new androidx.work.PeriodicWorkRequest.Builder(
                        com.app.smh.alarm.PendingCheckWorker.class,
                        15, java.util.concurrent.TimeUnit.MINUTES)
                        .build();

        androidx.work.WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "pending_check",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        workRequest);
    }
}
