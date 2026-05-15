package com.app.smh;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.app.smh.alarm.AlarmScheduler;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class AlarmSettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;

    private LinearLayout layoutMorningTime;
    private LinearLayout layoutLunchTime;
    private LinearLayout layoutDinnerTime;
    private LinearLayout layoutWakeupTime;
    private LinearLayout layoutBedtimeTime;

    private TextView tvMorningTime;
    private TextView tvLunchTime;
    private TextView tvDinnerTime;
    private TextView tvWakeupTime;
    private TextView tvBedtimeTime;
    private TextView tvWakeupLabel;
    private TextView tvBedtimeLabel;

    private SwitchCompat switchWakeup;
    private SwitchCompat switchBedtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        initViews();
        loadSavedSettings();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

        layoutMorningTime = findViewById(R.id.layout_morning_time);
        layoutLunchTime = findViewById(R.id.layout_lunch_time);
        layoutDinnerTime = findViewById(R.id.layout_dinner_time);
        layoutWakeupTime = findViewById(R.id.layout_wakeup_time);
        layoutBedtimeTime = findViewById(R.id.layout_bedtime_time);

        tvMorningTime = findViewById(R.id.tv_morning_time);
        tvLunchTime = findViewById(R.id.tv_lunch_time);
        tvDinnerTime = findViewById(R.id.tv_dinner_time);
        tvWakeupTime = findViewById(R.id.tv_wakeup_time);
        tvBedtimeTime = findViewById(R.id.tv_bedtime_time);

        tvWakeupLabel = findViewById(R.id.tv_wakeup_label);
        tvBedtimeLabel = findViewById(R.id.tv_bedtime_label);

        switchWakeup = findViewById(R.id.switch_wakeup);
        switchBedtime = findViewById(R.id.switch_bedtime);
    }

    /**
     * SettingsManager에서 저장된 24시간제 "HH:mm"을 불러와
     * 12시간제 "오전/오후 HH:mm" 형식으로 변환하여 표시
     * (기존: tvMorningTime.setText("13:00") → 수정: "오후 01:00")
     */
    private void loadSavedSettings() {
        // null이면 예시 힌트 표시, 아니면 저장된 12시간제 시간 표시
        String morningTime = SettingsManager.getAlarmMorningTime(this);
        String lunchTime = SettingsManager.getAlarmLunchTime(this);
        String dinnerTime = SettingsManager.getAlarmDinnerTime(this);
        String wakeupTime = SettingsManager.getAlarmWakeupTime(this);
        String bedtimeTime = SettingsManager.getAlarmBedtimeTime(this);

        tvMorningTime.setText(morningTime != null ? to12HourDisplay(morningTime) : "시간 설정");
        tvLunchTime.setText(lunchTime != null ? to12HourDisplay(lunchTime) : "시간 설정");
        tvDinnerTime.setText(dinnerTime != null ? to12HourDisplay(dinnerTime) : "시간 설정");
        tvWakeupTime.setText(wakeupTime != null ? to12HourDisplay(wakeupTime) : "시간 설정");
        tvBedtimeTime.setText(bedtimeTime != null ? to12HourDisplay(bedtimeTime) : "시간 설정");

        switchWakeup.setChecked(SettingsManager.isWakeupAlarmEnabled(this));
        switchBedtime.setChecked(SettingsManager.isBedtimeAlarmEnabled(this));

        updateOptionalSectionState();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // 아침
        layoutMorningTime.setOnClickListener(v ->
                showTimePicker("아침 복용 시간",
                        SettingsManager.getAlarmMorningTime(this),
                        (hour, minute) -> {
                            String time24 = to24HourString(hour, minute);
                            SettingsManager.setAlarmMorningTime(this, time24);
                            tvMorningTime.setText(to12HourDisplay(time24));
                            AlarmScheduler.scheduleAll(this);
                        })
        );

        // 점심
        layoutLunchTime.setOnClickListener(v ->
                showTimePicker("점심 복용 시간",
                        SettingsManager.getAlarmLunchTime(this),
                        (hour, minute) -> {
                            String time24 = to24HourString(hour, minute);
                            SettingsManager.setAlarmLunchTime(this, time24);
                            tvLunchTime.setText(to12HourDisplay(time24));
                            AlarmScheduler.scheduleAll(this);
                        })
        );

        // 저녁
        layoutDinnerTime.setOnClickListener(v ->
                showTimePicker("저녁 복용 시간",
                        SettingsManager.getAlarmDinnerTime(this),
                        (hour, minute) -> {
                            String time24 = to24HourString(hour, minute);
                            SettingsManager.setAlarmDinnerTime(this, time24);
                            tvDinnerTime.setText(to12HourDisplay(time24));
                            AlarmScheduler.scheduleAll(this);
                        })
        );

        // 기상 직후 (스위치 켜진 경우만 시간 변경 가능)
        layoutWakeupTime.setOnClickListener(v -> {
            if (!switchWakeup.isChecked()) return;
            showTimePicker("기상 직후 복용 시간",
                    SettingsManager.getAlarmWakeupTime(this),
                    (hour, minute) -> {
                        String time24 = to24HourString(hour, minute);
                        SettingsManager.setAlarmWakeupTime(this, time24);
                        tvWakeupTime.setText(to12HourDisplay(time24));
                        AlarmScheduler.scheduleAll(this);
                    });
        });

        // 취침 전 (스위치 켜진 경우만 시간 변경 가능)
        layoutBedtimeTime.setOnClickListener(v -> {
            if (!switchBedtime.isChecked()) return;
            showTimePicker("취침 전 복용 시간",
                    SettingsManager.getAlarmBedtimeTime(this),
                    (hour, minute) -> {
                        String time24 = to24HourString(hour, minute);
                        SettingsManager.setAlarmBedtimeTime(this, time24);
                        tvBedtimeTime.setText(to12HourDisplay(time24));
                        AlarmScheduler.scheduleAll(this);
                    });
        });

        // 기상 후 스위치
        switchWakeup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setWakeupAlarmEnabled(this, isChecked);
            updateOptionalSectionState();
            AlarmScheduler.scheduleAll(this);
        });

        // 취침 전 스위치
        switchBedtime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setBedtimeAlarmEnabled(this, isChecked);
            updateOptionalSectionState();
            AlarmScheduler.scheduleAll(this);
        });
    }

    private void updateOptionalSectionState() {
        boolean wakeupEnabled = switchWakeup.isChecked();
        boolean bedtimeEnabled = switchBedtime.isChecked();

        setOptionalSectionEnabled(layoutWakeupTime, tvWakeupLabel, tvWakeupTime, wakeupEnabled);
        setOptionalSectionEnabled(layoutBedtimeTime, tvBedtimeLabel, tvBedtimeTime, bedtimeEnabled);
    }

    private void setOptionalSectionEnabled(View layout, TextView label, TextView timeView, boolean enabled) {
        layout.setEnabled(enabled);
        layout.setClickable(enabled);

        float alpha = enabled ? 1.0f : 0.35f;
        label.setAlpha(alpha);
        timeView.setAlpha(alpha);
    }

    /**
     * 기존: TimePickerDialog (24시간제)
     * 변경: MaterialTimePicker (12시간제 오전/오후)
     *
     * picker.getHour()는 항상 0~23 반환
     * → to24HourString()으로 변환 후 SettingsManager에 저장
     * → to12HourDisplay()로 변환 후 TextView에 표시
     */
    private void showTimePicker(String title, String current, OnTimeSelectedCallback callback) {
        int hour = 8, minute = 0;
        try {
            String[] parts = current.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)  // 12시간제 (오전/오후)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(title)
                .build();

        picker.addOnPositiveButtonClickListener(v ->
                callback.onSelected(picker.getHour(), picker.getMinute())
        );

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    /**
     * 24시간제 "HH:mm" → 12시간제 표시 문자열
     * "08:00" → "오전 08:00"
     * "13:00" → "오후 01:00"
     * "00:00" → "오전 12:00"
     * "12:00" → "오후 12:00"
     */
    private String to12HourDisplay(String time24) {
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

    /**
     * hour(0~23), minute(0~59) → 24시간제 "HH:mm"
     * SettingsManager와 AlarmScheduler가 이 형식으로 저장/사용
     */
    private String to24HourString(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    interface OnTimeSelectedCallback {
        void onSelected(int hour, int minute);
    }
}
