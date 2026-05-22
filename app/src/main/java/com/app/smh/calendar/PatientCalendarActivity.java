package com.app.smh.calendar;

import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.app.smh.R;
import com.app.smh.SettingsManager;
import com.app.smh.auth.AuthApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PatientCalendarActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthTitle;
    private TextView tvPatientName;
    private TextView tvLegendDone;
    private TextView tvLegendProgress;
    private TextView tvSelectedDateTitle;
    private GridView gridCalendar;
    private LinearLayout layoutDetailList;

    private Calendar currentMonthCalendar;
    private Calendar selectedDateCalendar;

    private long caregiverId = -1L;
    private long patientId = -1L;
    private String patientName = "";

    private AuthApiClient authApiClient;

    // 서버에서 받은 월별 복약 기록 캐시
    // key: "yyyy-MM-dd", value: List<IntakeLogResponse>
    private final Map<String, List<AuthApiClient.IntakeLogResponse>> intakeLogMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_calendar);

        authApiClient = new AuthApiClient();
        caregiverId = SettingsManager.getLoginUserId(this);
        patientId = getIntent().getLongExtra("patientId", -1L);
        patientName = getIntent().getStringExtra("patientName") != null
                ? getIntent().getStringExtra("patientName") : "피보호자";

        initViews();

        currentMonthCalendar = Calendar.getInstance();
        selectedDateCalendar = Calendar.getInstance();

        tvPatientName.setText(patientName + "님의 복약 현황");

        btnBack.setOnClickListener(v -> finish());
        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            loadMonthlyData();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            loadMonthlyData();
        });

        // 이번 달 데이터 로드
        loadMonthlyData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        tvMonthTitle = findViewById(R.id.tv_month_title);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvLegendDone = findViewById(R.id.tv_legend_done);
        tvLegendProgress = findViewById(R.id.tv_legend_progress);
        tvSelectedDateTitle = findViewById(R.id.tv_selected_date_title);
        gridCalendar = findViewById(R.id.grid_calendar);
        layoutDetailList = findViewById(R.id.layout_detail_list);
    }

    /**
     * 서버에서 해당 월 피보호자 복약 기록 로드
     */
    private void loadMonthlyData() {
        int year = currentMonthCalendar.get(Calendar.YEAR);
        int month = currentMonthCalendar.get(Calendar.MONTH) + 1;

        new Thread(() -> {
            try {
                List<AuthApiClient.IntakeLogResponse> logs =
                        authApiClient.getPatientMonthlyLogs(patientId, year, month);

                // 날짜별로 Map에 분류
                intakeLogMap.clear();
                if (logs != null) {
                    for (AuthApiClient.IntakeLogResponse log : logs) {
                        if (log.date == null) continue;
                        intakeLogMap.computeIfAbsent(log.date, k -> new ArrayList<>()).add(log);
                    }
                }

                runOnUiThread(() -> {
                    renderCalendar();
                    renderSelectedDateDetails();
                });

            } catch (Exception e) {
                runOnUiThread(() -> renderCalendar());
            }
        }).start();
    }

    private void renderCalendar() {
        tvMonthTitle.setText(new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(currentMonthCalendar.getTime()));

        ArrayList<CalendarDayItem> dayItems = buildCalendarItems();
        CalendarGridAdapter adapter = new CalendarGridAdapter(dayItems, item -> {
            if (item.getDayNumber() <= 0) return;
            try {
                Calendar clicked = Calendar.getInstance();
                clicked.setTime(Objects.requireNonNull(
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .parse(item.getDateString())));
                selectedDateCalendar = clicked;
                renderSelectedDateDetails();
                renderCalendar();
            } catch (Exception ignored) {}
        });

        gridCalendar.setAdapter(adapter);
        updateLegend(dayItems);
    }

    private ArrayList<CalendarDayItem> buildCalendarItems() {
        ArrayList<CalendarDayItem> items = new ArrayList<>();

        Calendar monthCal = (Calendar) currentMonthCalendar.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1;
        int lastDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            items.add(new CalendarDayItem("", 0, false, false, false, false));
        }

        for (int day = 1; day <= lastDay; day++) {
            Calendar dayCal = (Calendar) monthCal.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, day);

            String dateString = formatDate(dayCal);
            boolean isToday = isSameDate(dayCal, Calendar.getInstance());
            boolean isDone = isDateAllCompleted(dateString);
            boolean isSelected = isSameDate(dayCal, selectedDateCalendar);

            items.add(new CalendarDayItem(
                    dateString, day, true, isToday, isDone, isSelected));
        }

        while (items.size() % 7 != 0) {
            items.add(new CalendarDayItem("", 0, false, false, false, false));
        }

        return items;
    }

    /**
     * 해당 날짜의 모든 복약이 TAKEN인지 확인
     */
    private boolean isDateAllCompleted(String date) {
        List<AuthApiClient.IntakeLogResponse> logs = intakeLogMap.get(date);
        if (logs == null || logs.isEmpty()) return false;
        for (AuthApiClient.IntakeLogResponse log : logs) {
            if (!"TAKEN".equals(log.status)) return false;
        }
        return true;
    }

    private void updateLegend(ArrayList<CalendarDayItem> dayItems) {
        int doneCount = 0;
        int progressCount = 0;

        String todayString = formatDate(Calendar.getInstance());

        for (CalendarDayItem item : dayItems) {
            if (item.getDayNumber() <= 0) continue;
            if (item.isDone()) doneCount++;

            List<AuthApiClient.IntakeLogResponse> logs =
                    intakeLogMap.get(item.getDateString());
            if (logs != null && !logs.isEmpty()
                    && item.getDateString().equals(todayString)
                    && !isDateAllCompleted(todayString)) {
                progressCount = 1;
            }
        }

        tvLegendDone.setText(doneCount + " 완료");
        tvLegendProgress.setText(progressCount + " 진행중");
    }

    private void renderSelectedDateDetails() {
        String title = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN)
                .format(selectedDateCalendar.getTime());
        tvSelectedDateTitle.setText(title);

        layoutDetailList.removeAllViews();

        String date = formatDate(selectedDateCalendar);
        List<AuthApiClient.IntakeLogResponse> logs = intakeLogMap.get(date);

        if (logs == null || logs.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("이 날짜에 복약 기록이 없습니다.");
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 16, 0, 16);
            layoutDetailList.addView(tvEmpty);
            return;
        }

        for (AuthApiClient.IntakeLogResponse log : logs) {
            View detailView = getLayoutInflater().inflate(
                    R.layout.item_calendar_schedule_detail, layoutDetailList, false);

            TextView tvCategory = detailView.findViewById(R.id.tv_detail_category_name);
            TextView tvTime = detailView.findViewById(R.id.tv_detail_time);
            TextView tvStatus = detailView.findViewById(R.id.tv_detail_status);

            tvCategory.setText(log.medicationName);
            tvTime.setText(toKoreanTimeSlot(log.timeSlot));

            if ("TAKEN".equals(log.status)) {
                detailView.setBackgroundResource(R.drawable.bg_schedule_item_done);
                tvStatus.setText("완료");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.main_coral));
                tvStatus.setBackgroundResource(R.drawable.bg_schedule_done_button_done);
            } else {
                detailView.setBackgroundResource(R.drawable.bg_schedule_item_pending);
                tvStatus.setText("미완료");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
                tvStatus.setBackgroundResource(R.drawable.bg_schedule_done_button_pending);
            }

            // 읽기 전용: 클릭 불가
            detailView.setClickable(false);
            detailView.setFocusable(false);

            layoutDetailList.addView(detailView);
        }
    }

    /**
     * 서버 TimeSlot → 한글 변환
     */
    private String toKoreanTimeSlot(String timeSlot) {
        if (timeSlot == null) return "";
        switch (timeSlot) {
            case "MORNING": return "아침";
            case "LUNCH":   return "점심";
            case "DINNER":  return "저녁";
            case "BEDTIME": return "취침 전";
            case "WAKEUP":  return "기상 후";
            default:        return timeSlot;
        }
    }

    private boolean isSameDate(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    private String formatDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.getTime());
    }
}

