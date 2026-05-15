package com.app.smh;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.app.smh.schedule.ScheduleRepository;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (SettingsManager.isDarkModeEnabled(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_splash_logo);
        TextView title = findViewById(R.id.tv_splash_title);

        if (logo == null || title == null) {
            // 뷰를 찾지 못한 경우 바로 분기 처리
            goToNext();
            return;
        }

        // 로고 드롭 + 회전 + 페이드인 애니메이션 (기존 유지)
        ObjectAnimator drop = ObjectAnimator.ofFloat(logo, "translationY", -1200f, 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(logo, "rotation", -20f, 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(drop, rotate, fadeIn);
        animSet.setDuration(1200);
        animSet.setInterpolator(new BounceInterpolator());
        animSet.start();

        // 타이틀 슬라이드 + 회전 + 페이드인 애니메이션 (기존 유지)
        ObjectAnimator textSlide = ObjectAnimator.ofFloat(title, "translationX", -900f, 0f);
        ObjectAnimator textRoll = ObjectAnimator.ofFloat(title, "rotation", -720f, 0f);
        ObjectAnimator textFadeIn = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);

        AnimatorSet textAnimSet = new AnimatorSet();
        textAnimSet.playTogether(textSlide, textRoll, textFadeIn);
        textAnimSet.setDuration(1100);
        textAnimSet.setStartDelay(250);
        textAnimSet.setInterpolator(new BounceInterpolator());
        textAnimSet.start();

        // 애니메이션 종료(2500ms) 후 로그인 상태 확인하여 화면 분기
        logo.postDelayed(this::goToNext, 2500);
    }

    private void goToNext() {
        // SettingsManager에서 저장된 userId로 로그인 상태 확인
        if (SettingsManager.isLoggedIn(this)) {
            // 로그인 상태 → 서버에서 복약 스케줄 동기화 후 메인화면으로 이동
            ScheduleRepository.syncFromServer(this, () ->
                    runOnUiThread(() -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
            );
        } else {
            // 로그인 정보 없음 → 로그인 화면으로 이동 (기존 동작 유지)
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
