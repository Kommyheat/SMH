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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.app.smh.R;
import com.app.smh.scan.DrugInfoApiManager;
import com.app.smh.scan.DrugResultItem;
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

    //날짜별 완료 상태 확인 .. 5/9에 완료해도 5/10~5/12는 미완료 유지

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

    // 완료 상태 표시를 날짜 기준
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
                tvCategory.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tvTime.setTextColor(ContextCompat.getColor(this, R.color.white));
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
        com.google.android.material.chip.ChipGroup chipGroupDrugs =
                sheetView.findViewById(R.id.chip_group_drugs);
        ScrollView scrollBsDetail = sheetView.findViewById(R.id.scroll_bs_detail);
        LinearLayout layoutDrugList = sheetView.findViewById(R.id.layout_bs_drug_list);
        android.widget.EditText etBsMemo = sheetView.findViewById(R.id.et_bs_memo);
        LinearLayout btnBsSaveMemo = sheetView.findViewById(R.id.btn_bs_save_memo);

        tvBsDrugName.setText(scheduleItem.getCategoryName());
        tvBsTimeSlot.setText(scheduleItem.getTimeSlot() + " 복용");

        // 기존 메모 불러오기
        String existingMemo = scheduleItem.getMemo();
        if (existingMemo != null && !existingMemo.isEmpty()) {
            etBsMemo.setText(existingMemo);
        }

        // 초기 상태: 저장 버튼 숨김
        btnBsSaveMemo.setVisibility(View.GONE);

        // 텍스트 입력 시 저장 버튼 표시
        etBsMemo.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 입력 시작하면 저장 버튼 표시
                btnBsSaveMemo.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnBsSaveMemo.setOnClickListener(v -> {
            String memo = etBsMemo.getText().toString();
            scheduleItem.setMemo(memo);
            ScheduleRepository.updateCompleted(this, scheduleItem);

            // 저장 후 버튼 숨기기
            btnBsSaveMemo.setVisibility(View.GONE);

            // 포커스 제거 → 깜빡이 사라짐
            etBsMemo.clearFocus();

            // 키보드 내리기
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etBsMemo.getWindowToken(), 0);
            }

            android.widget.Toast.makeText(this, "메모가 저장되었습니다.",
                    android.widget.Toast.LENGTH_SHORT).show();
        });


        List<ScheduleMedicineItem.DrugDetail> details = scheduleItem.getDrugDetails();

        if (details != null && !details.isEmpty()) {
            progressBs.setVisibility(View.GONE);
            setupDrugChips(chipGroupDrugs, layoutDrugList,
                    scrollBsDetail, tvBsError, details);
        } else {
            progressBs.setVisibility(View.VISIBLE);
            tvBsError.setVisibility(View.GONE);
            scrollBsDetail.setVisibility(View.GONE);
            chipGroupDrugs.setVisibility(View.GONE);

            ArrayList<String> drugNames = scheduleItem.getDrugNames();
            if (drugNames == null || drugNames.isEmpty()) {
                drugNames = new ArrayList<>();
                drugNames.add(scheduleItem.getCategoryName());
            }

            DrugInfoApiManager apiManager = new DrugInfoApiManager();
            List<ScheduleMedicineItem.DrugDetail> fetchedDetails = new ArrayList<>();
            final int[] remaining = {drugNames.size()};

            for (String drugName : drugNames) {
                apiManager.fetchDrugDetail(drugName,
                        new DrugInfoApiManager.DetailCallback() {
                            @Override
                            public void onSuccess(DrugResultItem result) {
                                if (result.hasDetail()) {
                                    ScheduleMedicineItem.DrugDetail detail =
                                            new ScheduleMedicineItem.DrugDetail();
                                    detail.recognizedName = drugName;
                                    detail.itemName = result.getItemName();
                                    detail.entpName = result.getEntpName();
                                    detail.efcyQesitm = result.getEfcyQesitm();
                                    detail.useMethodQesitm = result.getUseMethodQesitm();
                                    detail.atpnWarnQesitm = result.getAtpnWarnQesitm();
                                    detail.depositMethodQesitm = result.getDepositMethodQesitm();
                                    fetchedDetails.add(detail);
                                }
                                remaining[0]--;
                                if (remaining[0] <= 0) {
                                    progressBs.setVisibility(View.GONE);
                                    if (fetchedDetails.isEmpty()) {
                                        tvBsError.setText("약 상세정보를 찾을 수 없습니다.");
                                        tvBsError.setVisibility(View.VISIBLE);
                                    } else {
                                        setupDrugChips(chipGroupDrugs, layoutDrugList,
                                                scrollBsDetail, tvBsError, fetchedDetails);
                                        scheduleItem.setDrugDetails(fetchedDetails);
                                        ScheduleRepository.updateDrugDetails(
                                                MedicationCalendarActivity.this, scheduleItem);
                                    }
                                }
                            }

                            @Override
                            public void onError(String message) {
                                remaining[0]--;
                                if (remaining[0] <= 0) {
                                    progressBs.setVisibility(View.GONE);
                                    tvBsError.setText("약 상세정보 조회에 실패했습니다.");
                                    tvBsError.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        }

        bottomSheet.show();
    }

    // 약품명 Chip 버튼 생성 + 클릭 시 상세정보 표시

    private void setupDrugChips(
            com.google.android.material.chip.ChipGroup chipGroup,
            LinearLayout layoutDrugList,
            ScrollView scrollBsDetail,
            TextView tvBsError,
            List<ScheduleMedicineItem.DrugDetail> details) {

        chipGroup.removeAllViews();
        chipGroup.setVisibility(View.VISIBLE);
        scrollBsDetail.setVisibility(View.GONE);
        tvBsError.setVisibility(View.GONE);

        for (ScheduleMedicineItem.DrugDetail detail : details) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(this);
            chip.setText(detail.recognizedName);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeColorResource(R.color.main_coral);
            chip.setChipStrokeWidth(2f);
            chip.setTextColor(getColor(R.color.black));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // 선택된 Chip 코럴색
                    chip.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#FF786E")));
                    chip.setTextColor(android.graphics.Color.WHITE);

                    // 상세정보 표시
                    showSingleDrugDetail(layoutDrugList, scrollBsDetail, detail);
                } else {
                    // 선택 해제 흰색
                    chip.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.WHITE));
                    chip.setTextColor(getColor(R.color.black));
                }
            });

            chipGroup.addView(chip);
        }

        // 약품이 1개면 자동 선택
        if (details.size() == 1) {
            com.google.android.material.chip.Chip firstChip =
                    (com.google.android.material.chip.Chip) chipGroup.getChildAt(0);
            if (firstChip != null) firstChip.setChecked(true);
        }
    }

    // 선택된 약품 1개 상세정보 표시
    private void showSingleDrugDetail(LinearLayout layoutDrugList,
                                      ScrollView scrollBsDetail,
                                      ScheduleMedicineItem.DrugDetail detail) {
        scrollBsDetail.setVisibility(View.VISIBLE);
        layoutDrugList.removeAllViews();

        addDetailRow(layoutDrugList, "품목명", detail.itemName);
        addDetailRow(layoutDrugList, "제조사", detail.entpName);
        addDetailRow(layoutDrugList, "효능/효과", detail.efcyQesitm);
        addDetailRow(layoutDrugList, "용법/용량", detail.useMethodQesitm);
        addDetailRow(layoutDrugList, "주의사항", detail.atpnWarnQesitm);
        addDetailRow(layoutDrugList, "보관방법", detail.depositMethodQesitm);
    }

    // 약품 상세정보 목록 표시 (공통 메서드)
    private void showDrugDetails(LinearLayout layoutDrugList, ScrollView scrollBsDetail,
                                 TextView tvBsError,
                                 List<ScheduleMedicineItem.DrugDetail> details) {
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
