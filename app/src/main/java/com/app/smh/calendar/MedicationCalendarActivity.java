package com.app.smh.calendar;

import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.app.smh.R;
import com.app.smh.schedule.ScheduleMedicineItem;
import com.app.smh.schedule.ScheduleRepository;
import com.app.smh.SettingsManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MedicationCalendarActivity extends     AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthTitle;
    private TextView tvLegendDone;
    private TextView tvLegendProgress;
    private TextView tvSelectedDateTitle;
    private GridView gridCalendar;
    private LinearLayout layoutDetailList;

    private Calendar currentMonthCalendar;
    private Calendar selectedDateCalendar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_calendar);

        initViews();

        currentMonthCalendar = Calendar.getInstance();
        selectedDateCalendar = Calendar.getInstance();

        btnBack.setOnClickListener(v -> finish());
        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            renderCalendar();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            renderCalendar();
        });
        tvMonthTitle.setOnClickListener(v -> showMonthYearPickerDialog());

        renderCalendar();
        renderSelectedDateDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        tvMonthTitle = findViewById(R.id.tv_month_title);
        tvLegendDone = findViewById(R.id.tv_legend_done);
        tvLegendProgress = findViewById(R.id.tv_legend_progress);
        tvSelectedDateTitle = findViewById(R.id.tv_selected_date_title);
        gridCalendar = findViewById(R.id.grid_calendar);
        layoutDetailList = findViewById(R.id.layout_detail_list);
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

            items.add(new CalendarDayItem(dateString, day, true, isToday, isDone, isSelected));
        }

        while (items.size() % 7 != 0) {
            items.add(new CalendarDayItem("", 0, false, false, false, false));
        }

        return items;
    }

    /**
     * 수정: 날짜별 완료 상태 확인
     * isCompleted() → isCompletedOn(date) 로 변경
     * 5/9에 완료해도 5/10~5/12는 미완료 유지
     */
    private boolean isDateAllCompleted(String date) {
        ArrayList<ScheduleMedicineItem> items =
                ScheduleRepository.getSchedulesByDate(this, date);
        if (items.isEmpty()) return false;
        for (ScheduleMedicineItem item : items) {
            if (!item.isCompletedOn(date)) return false;

        }
        return true;
    }

    private void updateLegend(ArrayList<CalendarDayItem> dayItems) {
        int doneCount = 0;
        int progressCount = 0;

        Calendar today = Calendar.getInstance();
        String todayString = formatDate(today);

        for (CalendarDayItem item : dayItems) {
            if (item.getDayNumber() <= 0) continue;
            if (item.isDone()) doneCount++;

            ArrayList<ScheduleMedicineItem> schedules =
                    ScheduleRepository.getSchedulesByDate(this, item.getDateString());
            if (!schedules.isEmpty()
                    && item.getDateString().equals(todayString)
                    && !isDateAllCompleted(todayString)) {
                progressCount = 1;
            }
        }

        tvLegendDone.setText(doneCount + " 완료");
        tvLegendProgress.setText(progressCount + " 진행중");
    }

    /**
     * 수정: 완료 상태 표시를 날짜 기준으로 변경
     * item.isCompleted() → item.isCompletedOn(date)
     */
    private void renderSelectedDateDetails() {
        String title = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN)
                .format(selectedDateCalendar.getTime());
        tvSelectedDateTitle.setText(title);

        layoutDetailList.removeAllViews();

        String date = formatDate(selectedDateCalendar);
        ArrayList<ScheduleMedicineItem> items =
                ScheduleRepository.getSchedulesByDate(this, date);

        if (items.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("이 날짜에 등록된 복약 일정이 없습니다.");
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 16, 0, 16);
            layoutDetailList.addView(tvEmpty);
            return;
        }

        for (ScheduleMedicineItem item : items) {
            View detailView = getLayoutInflater().inflate(
                    R.layout.item_calendar_schedule_detail, layoutDetailList, false);

            TextView tvCategory = detailView.findViewById(R.id.tv_detail_category_name);
            TextView tvTime = detailView.findViewById(R.id.tv_detail_time);
            TextView tvStatus = detailView.findViewById(R.id.tv_detail_status);

            tvCategory.setText(item.getCategoryName());
            tvTime.setText(item.getTimeSlot());

            // 해당 날짜 기준으로 완료 상태 표시
            if (item.isCompletedOn(date)) {
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


            detailView.setClickable(true);
            detailView.setFocusable(true);
            detailView.setOnClickListener(v -> showDrugDetailBottomSheet(item));

            layoutDetailList.addView(detailView);
        }
    }

    private void showDrugDetailBottomSheet(ScheduleMedicineItem scheduleItem) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(
                R.layout.bottom_sheet_drug_detail, null);
        bottomSheet.setContentView(sheetView);

        TextView tvBsDrugName = sheetView.findViewById(R.id.tv_bs_drug_name);
        TextView tvBsTimeSlot = sheetView.findViewById(R.id.tv_bs_time_slot);
        ProgressBar progressBs = sheetView.findViewById(R.id.progress_bs);
        TextView tvBsError = sheetView.findViewById(R.id.tv_bs_error);
        ScrollView scrollBsDetail = sheetView.findViewById(R.id.scroll_bs_detail);
        LinearLayout layoutDrugList = sheetView.findViewById(R.id.layout_bs_drug_list);

        tvBsDrugName.setText(scheduleItem.getCategoryName());
        tvBsTimeSlot.setText(scheduleItem.getTimeSlot() + " 복용");
        progressBs.setVisibility(View.GONE);

        List<ScheduleMedicineItem.DrugDetail> details = scheduleItem.getDrugDetails();

        if (details == null || details.isEmpty()) {
            tvBsError.setText("저장된 약 상세정보가 없습니다.");
            tvBsError.setVisibility(View.VISIBLE);
            scrollBsDetail.setVisibility(View.GONE);
        } else {
            tvBsError.setVisibility(View.GONE);
            scrollBsDetail.setVisibility(View.VISIBLE);
            layoutDrugList.removeAllViews();

            for (ScheduleMedicineItem.DrugDetail detail : details) {
                TextView tvDrugHeader = new TextView(this);
                tvDrugHeader.setText("■ " + detail.recognizedName);
                tvDrugHeader.setTextColor(0xFF111111);
                tvDrugHeader.setTextSize(15f);
                tvDrugHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                tvDrugHeader.setPadding(0, 16, 0, 8);
                layoutDrugList.addView(tvDrugHeader);

                addDetailRow(layoutDrugList, "품목명", detail.itemName);
                addDetailRow(layoutDrugList, "제조사", detail.entpName);
                addDetailRow(layoutDrugList, "효능/효과", detail.efcyQesitm);
                addDetailRow(layoutDrugList, "용법/용량", detail.useMethodQesitm);
                addDetailRow(layoutDrugList, "주의사항", detail.atpnWarnQesitm);
                addDetailRow(layoutDrugList, "보관방법", detail.depositMethodQesitm);

                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(0, 8, 0, 8);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(0xFFF0F0F0);
                layoutDrugList.addView(divider);
            }
        }

        bottomSheet.show();
    }

    private void addDetailRow(LinearLayout parent, String label, String value) {
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(0xFFFF786E);
        tvLabel.setTextSize(12f);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setPadding(0, 8, 0, 2);
        parent.addView(tvLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText((value == null || value.isEmpty() || "-".equals(value))
                ? "정보 없음" : value);
        tvValue.setTextColor(0xFF222222);
        tvValue.setTextSize(14f);
        tvValue.setPadding(0, 0, 0, 4);
        parent.addView(tvValue);
    }

    private void showMonthYearPickerDialog() {
        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_month_year_picker, null);

        NumberPicker pickerYear = dialogView.findViewById(R.id.picker_year);
        NumberPicker pickerMonth = dialogView.findViewById(R.id.picker_month);

        int currentYear = currentMonthCalendar.get(Calendar.YEAR);
        int currentMonth = currentMonthCalendar.get(Calendar.MONTH) + 1;

        pickerYear.setMinValue(2020);
        pickerYear.setMaxValue(2035);
        pickerYear.setValue(currentYear);

        pickerMonth.setMinValue(1);
        pickerMonth.setMaxValue(12);
        pickerMonth.setValue(currentMonth);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("확인", (dialog, which) -> {
                    int selectedYear = pickerYear.getValue();
                    int selectedMonth = pickerMonth.getValue();
                    currentMonthCalendar.set(Calendar.YEAR, selectedYear);
                    currentMonthCalendar.set(Calendar.MONTH, selectedMonth - 1);
                    currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    renderCalendar();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private String safeText(String value) {
        return (value == null || value.isEmpty() || "-".equals(value))
                ? "정보 없음" : value;
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


    private Long getUserId() {
        long userId = SettingsManager.getLoginUserId(this);
        return userId > 0 ? userId : null;
    }
}
