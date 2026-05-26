package com.app.smh;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.smh.alarm.MedicationAlarmReceiver;
import com.app.smh.calendar.MedicationCalendarActivity;
import com.app.smh.health.TodayHealthManager;
import com.app.smh.scan.ScanCameraFragment;
import com.app.smh.schedule.IntakeServerSync;
import com.app.smh.schedule.ManualRegisterDialogFragment;
import com.app.smh.schedule.MedicationServerSync;
import com.app.smh.schedule.ScheduleMedicineItem;
import com.app.smh.schedule.ScheduleRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppCompatImageButton fabChatbot;
    private BottomNavigationView bottomNavigationView;
    private TextView tvBannerMessage;
    private View mainScroll;
    private FrameLayout fragmentContainer;
    private TextView tvSelectedDate;
    private ImageButton btnPrevDate;
    private ImageButton btnNextDate;
    private LinearLayout layoutMorningMedicineList;
    private LinearLayout layoutLunchMedicineList;
    private LinearLayout layoutDinnerMedicineList;
    private TextView tvEmptyMorning;
    private TextView tvEmptyLunch;
    private TextView tvEmptyDinner;
    private Calendar selectedCalendar;
    private ImageButton btnCalendar;
    private View dimOverlay;
    private View todayHealthPanel;
    private boolean isHealthPanelVisible = false;

    // 선택 모드 필드
    private boolean isSelectionMode = false;
    private final HashSet<String> selectedKeys = new HashSet<>();
    private LinearLayout layoutSelectionToolbar;
    private TextView tvSelectionCount;
    private TextView btnSelectAll;
    private TextView btnDeleteSelected;
    private TextView btnCancelSelection;

    private String makeItemKey(ScheduleMedicineItem item) {
        return item.getCategoryName() + "_"
                + item.getStartDate() + "_"
                + item.getTimeSlot();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isSelectionMode) {
                            exitSelectionMode();
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        initViews();
        TextView logoTitle = findViewById(R.id.tv_logo_title);

        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            logoTitle.setVisibility(View.GONE);
        } else {
            logoTitle.setVisibility(View.VISIBLE);
        }

        setupBottomNavigation();
        setupFabChatbot();
        setupCalendarButton();
        setupHealthButton();
        applyHomeBannerMessage();
        setupDateNavigation();
        setupManualRegisterButton();
        setupSelectionToolbar();
        handleRequestedTab(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyHomeBannerMessage();
        renderScheduleForSelectedDate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRequestedTab(intent);
    }

    private void initViews() {
        fabChatbot = findViewById(R.id.fab_chatbot);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        tvBannerMessage = findViewById(R.id.tv_banner_message);
        mainScroll = findViewById(R.id.main_scroll);
        fragmentContainer = findViewById(R.id.fragment_container);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        btnPrevDate = findViewById(R.id.btn_prev_date);
        btnNextDate = findViewById(R.id.btn_next_date);
        layoutMorningMedicineList = findViewById(R.id.layout_morning_medicine_list);
        layoutLunchMedicineList = findViewById(R.id.layout_lunch_medicine_list);
        layoutDinnerMedicineList = findViewById(R.id.layout_dinner_medicine_list);
        tvEmptyMorning = findViewById(R.id.tv_empty_morning);
        tvEmptyLunch = findViewById(R.id.tv_empty_lunch);
        tvEmptyDinner = findViewById(R.id.tv_empty_dinner);
        btnCalendar = findViewById(R.id.btn_calendar);
        selectedCalendar = Calendar.getInstance();
        dimOverlay = findViewById(R.id.dim_overlay);
        todayHealthPanel = findViewById(R.id.layout_today_health_panel);
        layoutSelectionToolbar = findViewById(R.id.layout_selection_toolbar);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnCancelSelection = findViewById(R.id.btn_cancel_selection);
    }

    // 선택 모드 툴바 (다이얼로그 1번만)
    private void setupSelectionToolbar() {
        if (btnCancelSelection != null) {
            btnCancelSelection.setOnClickListener(v -> exitSelectionMode());
        }
        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> selectAll());
        }
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> {
                if (selectedKeys.isEmpty()) {
                    Toast.makeText(this, "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 삭제할 아이템 수집
                ArrayList<ScheduleMedicineItem> all = ScheduleRepository.getAllSchedules(this);
                final ArrayList<ScheduleMedicineItem> toDelete = new ArrayList<>();
                for (ScheduleMedicineItem item : all) {
                    if (selectedKeys.contains(makeItemKey(item))) {
                        toDelete.add(item);
                    }
                }

                Log.d("DeleteTest", "toDelete 수: " + toDelete.size());

                if (toDelete.isEmpty()) {
                    Toast.makeText(this, "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 다이얼로그 1번만 표시
                new MaterialAlertDialogBuilder(this)
                        .setTitle("선택 삭제")
                        .setMessage(toDelete.size() + "개의 복약 일정을 삭제할까요?")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("삭제", (dialog, which) ->
                                executeDelete(toDelete))
                        .show();
            });
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedKeys.clear();
        if (layoutSelectionToolbar != null) layoutSelectionToolbar.setVisibility(View.VISIBLE);
        updateSelectionCount();
        renderScheduleForSelectedDate();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedKeys.clear();
        if (layoutSelectionToolbar != null) layoutSelectionToolbar.setVisibility(View.GONE);
        renderScheduleForSelectedDate();
    }

    private void selectAll() {
        String selectedDate = getSelectedDateString();
        ArrayList<ScheduleMedicineItem> schedules =
                ScheduleRepository.getSchedulesByDate(this, selectedDate);
        selectedKeys.clear();
        for (ScheduleMedicineItem item : schedules) {
            selectedKeys.add(makeItemKey(item));
        }
        updateSelectionCount();
        renderScheduleForSelectedDate();
    }

    private void updateSelectionCount() {
        if (tvSelectionCount != null) {
            tvSelectionCount.setText(selectedKeys.size() + "개 선택됨");
        }
    }

    // 실제 삭제 실행 (다이얼로그 없음, 1번만 호출)
    private void executeDelete(ArrayList<ScheduleMedicineItem> toDelete) {
        int total = toDelete.size();
        Log.d("DeleteTest", "executeDelete 시작: " + total + "개");

        MedicationServerSync.deleteMedicationBatch(this, toDelete, () ->
                runOnUiThread(() -> {
                    exitSelectionMode();
                    Toast.makeText(this, total + "개 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                })
        );
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showHomeContent();
                renderScheduleForSelectedDate();
                return true;
            } else if (id == R.id.nav_scan) {
                showScanFragment();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void showHomeContent() {
        if (mainScroll != null) mainScroll.setVisibility(View.VISIBLE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        if (fabChatbot != null) fabChatbot.setVisibility(View.VISIBLE);
    }

    private void showScanFragment() {
        if (mainScroll != null) mainScroll.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        if (fabChatbot != null) fabChatbot.setVisibility(View.GONE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ScanCameraFragment())
                .commit();
    }

    private void setupFabChatbot() {
        if (fabChatbot == null) return;
        fabChatbot.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatbotActivity.class)));
    }

    private void setupCalendarButton() {
        if (btnCalendar == null) return;
        btnCalendar.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MedicationCalendarActivity.class)));
    }

    private void setupHealthButton() {
        ImageView ivFavorite = findViewById(R.id.iv_favorite);
        if (ivFavorite == null) return;
        ivFavorite.setOnClickListener(v -> {
            if (isHealthPanelVisible) hideHealthPanel(ivFavorite);
            else showHealthPanel(ivFavorite);
        });
        if (dimOverlay != null) {
            dimOverlay.setOnClickListener(v ->
                    hideHealthPanel(findViewById(R.id.iv_favorite)));
        }
    }

    private void showHealthPanel(ImageView heartIcon) {
        if (heartIcon != null) {
            heartIcon.setImageResource(R.drawable.ic_heart_filled);
            heartIcon.clearColorFilter();
        }
        if (dimOverlay != null) {
            dimOverlay.setVisibility(View.VISIBLE);
            dimOverlay.setAlpha(0f);
            dimOverlay.animate().alpha(1f).setDuration(200).start();
        }
        bindHealthPanelData();
        if (todayHealthPanel != null) {
            todayHealthPanel.setVisibility(View.VISIBLE);
            todayHealthPanel.setAlpha(0f);
            todayHealthPanel.setTranslationY(-20f);
            todayHealthPanel.animate().alpha(1f).translationY(0f).setDuration(250).start();
        }
        isHealthPanelVisible = true;
    }

    private void hideHealthPanel(ImageView heartIcon) {
        if (heartIcon != null) {
            heartIcon.setImageResource(R.drawable.ic_heart);
            heartIcon.clearColorFilter();
        }
        if (dimOverlay != null) {
            dimOverlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> dimOverlay.setVisibility(View.GONE)).start();
        }
        if (todayHealthPanel != null) {
            todayHealthPanel.animate().alpha(0f).translationY(-20f).setDuration(200)
                    .withEndAction(() -> todayHealthPanel.setVisibility(View.GONE)).start();
        }
        isHealthPanelVisible = false;
    }

    private void bindHealthPanelData() {
        if (todayHealthPanel == null) return;
        TextView tvUpcoming = todayHealthPanel.findViewById(R.id.tv_upcoming_medicine);
        ImageButton btnClose = todayHealthPanel.findViewById(R.id.btn_close_health);
        if (btnClose != null) {
            btnClose.setOnClickListener(v ->
                    hideHealthPanel(findViewById(R.id.iv_favorite)));
        }
        if (tvUpcoming == null) return;
        TodayHealthManager.UpcomingMedicineInfo info =
                TodayHealthManager.getUpcomingMedicine(this);
        if (info == null) {
            tvUpcoming.setText("오늘 복용할 약이 없습니다.");
        } else {
            tvUpcoming.setText(TodayHealthManager.buildUpcomingMessage(info));
        }
    }

    private void applyHomeBannerMessage() {
        String message = SettingsManager.getHomeMessage(this);
        tvBannerMessage.setText(message);
    }

    private void setupDateNavigation() {
        if (btnPrevDate != null) {
            btnPrevDate.setOnClickListener(v -> {
                selectedCalendar.add(Calendar.DAY_OF_MONTH, -1);
                renderScheduleForSelectedDate();
            });
        }
        if (btnNextDate != null) {
            btnNextDate.setOnClickListener(v -> {
                selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
                renderScheduleForSelectedDate();
            });
        }
    }

    private void handleRequestedTab(Intent intent) {
        if (intent == null) return;
        String completeTimeSlot = intent.getStringExtra(
                MedicationAlarmReceiver.EXTRA_COMPLETE_TIME_SLOT);
        if (completeTimeSlot != null && !completeTimeSlot.isEmpty()) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            showHomeContent();
            renderScheduleForSelectedDate();
            markTimeSlotAsCompleted(completeTimeSlot);
            return;
        }
        String openTab = intent.getStringExtra("open_tab");
        if ("scan".equals(openTab)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_scan);
            showScanFragment();
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            showHomeContent();
            renderScheduleForSelectedDate();
        }
    }

    private void renderScheduleForSelectedDate() {
        String selectedDate = getSelectedDateString();
        if (tvSelectedDate != null) {
            tvSelectedDate.setText(isToday(selectedCalendar) ? "오늘" : selectedDate);
        }
        if (layoutMorningMedicineList != null) layoutMorningMedicineList.removeAllViews();
        if (layoutLunchMedicineList != null) layoutLunchMedicineList.removeAllViews();
        if (layoutDinnerMedicineList != null) layoutDinnerMedicineList.removeAllViews();

        ArrayList<ScheduleMedicineItem> schedules =
                ScheduleRepository.getSchedulesByDate(this, selectedDate);
        ArrayList<ScheduleMedicineItem> morningList = new ArrayList<>();
        ArrayList<ScheduleMedicineItem> lunchList = new ArrayList<>();
        ArrayList<ScheduleMedicineItem> dinnerList = new ArrayList<>();

        for (ScheduleMedicineItem item : schedules) {
            if (item == null || item.getTimeSlot() == null) continue;
            String ts = item.getTimeSlot().trim();
            if ("아침".equals(ts)) morningList.add(item);
            else if ("점심".equals(ts)) lunchList.add(item);
            else if ("저녁".equals(ts)) dinnerList.add(item);
        }

        renderTimeSlotList(layoutMorningMedicineList, tvEmptyMorning, morningList, "아침");
        renderTimeSlotList(layoutLunchMedicineList, tvEmptyLunch, lunchList, "점심");
        renderTimeSlotList(layoutDinnerMedicineList, tvEmptyDinner, dinnerList, "저녁");
    }

    private void renderTimeSlotList(LinearLayout container, TextView emptyView,
                                    ArrayList<ScheduleMedicineItem> items, String timeSlot) {
        if (container == null || emptyView == null) return;
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        String currentDate = getSelectedDateString();

        for (ScheduleMedicineItem item : items) {
            View itemView = inflater.inflate(
                    R.layout.item_schedule_medicine, container, false);

            TextView tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            TextView btnTakeDone = itemView.findViewById(R.id.btn_take_done);
            View foreground = itemView.findViewById(R.id.layout_swipe_foreground);
            CheckBox checkbox = itemView.findViewById(R.id.checkbox_item);

            if (tvCategoryName != null) tvCategoryName.setText(item.getCategoryName());

            boolean doneToday = item.isCompletedOn(currentDate);
            updateTakeButtonStyle(foreground, tvCategoryName, btnTakeDone, doneToday);

            if (isSelectionMode) {
                if (checkbox != null) {
                    checkbox.setVisibility(View.VISIBLE);
                    checkbox.setChecked(selectedKeys.contains(makeItemKey(item)));
                    if (doneToday) {
                        checkbox.setButtonTintList(
                                android.content.res.ColorStateList.valueOf(
                                        android.graphics.Color.WHITE));
                    } else {
                        checkbox.setButtonTintList(
                                android.content.res.ColorStateList.valueOf(
                                        android.graphics.Color.parseColor("#FF786E")));
                    }
                    checkbox.setOnCheckedChangeListener((btn, isChecked) -> {
                        if (isChecked) selectedKeys.add(makeItemKey(item));
                        else selectedKeys.remove(makeItemKey(item));
                        updateSelectionCount();
                    });
                }
                if (btnTakeDone != null) btnTakeDone.setEnabled(false);
                itemView.setOnClickListener(v -> {
                    String key = makeItemKey(item);
                    if (selectedKeys.contains(key)) {
                        selectedKeys.remove(key);
                        if (checkbox != null) checkbox.setChecked(false);
                    } else {
                        selectedKeys.add(key);
                        if (checkbox != null) checkbox.setChecked(true);
                    }
                    updateSelectionCount();
                });

            } else {
                if (checkbox != null) {
                    checkbox.setVisibility(View.GONE);
                    checkbox.setOnCheckedChangeListener(null);
                }
                if (btnTakeDone != null) btnTakeDone.setEnabled(true);

                if (btnTakeDone != null) {
                    btnTakeDone.setOnClickListener(v -> {
                        boolean current = item.isCompletedOn(currentDate);
                        item.setCompletedOn(currentDate, !current);
                        ScheduleRepository.updateCompletedForDate(this, item, currentDate);
                        updateTakeButtonStyle(foreground, tvCategoryName, btnTakeDone, !current);
                        if (!current) {
                            IntakeServerSync.syncTaken(this,
                                    item.getCategoryName(), item.getTimeSlot(), currentDate);
                        } else {
                            IntakeServerSync.syncCanceled(this,
                                    item.getCategoryName(), item.getTimeSlot(), currentDate);
                        }
                    });
                }

                itemView.setOnLongClickListener(v -> {
                    enterSelectionMode();
                    selectedKeys.add(makeItemKey(item));
                    updateSelectionCount();
                    renderScheduleForSelectedDate();
                    return true;
                });
            }
            container.addView(itemView);
        }
    }

    private void updateTakeButtonStyle(View foreground, TextView tvCategoryName,
                                       TextView btnTakeDone, boolean isCompleted) {
        if (foreground == null || btnTakeDone == null) return;
        if (isCompleted) {
            foreground.setBackgroundResource(R.drawable.bg_schedule_item_done);
            if (tvCategoryName != null)
                tvCategoryName.setTextColor(
                        ContextCompat.getColor(this, android.R.color.white));
            btnTakeDone.setText("완료");
            btnTakeDone.setTextColor(ContextCompat.getColor(this, R.color.main_coral));
            btnTakeDone.setBackgroundResource(R.drawable.bg_schedule_done_button_done);
        } else {
            foreground.setBackgroundResource(R.drawable.bg_schedule_item_pending);
            if (tvCategoryName != null)
                tvCategoryName.setTextColor(
                        ContextCompat.getColor(this, R.color.dark_gray));
            btnTakeDone.setText("미완료");
            btnTakeDone.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
            btnTakeDone.setBackgroundResource(R.drawable.bg_schedule_done_button_pending);
        }
    }

    private void setupManualRegisterButton() {
        ImageButton btnAddMedicine = findViewById(R.id.btn_add_medicine);
        if (btnAddMedicine == null) return;
        btnAddMedicine.setOnClickListener(v ->
                ManualRegisterDialogFragment.newInstance(() ->
                        renderScheduleForSelectedDate()
                ).show(getSupportFragmentManager(), "manual_register")
        );
    }

    private void markTimeSlotAsCompleted(String timeSlot) {
        if (timeSlot == null || timeSlot.isEmpty()) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        ArrayList<ScheduleMedicineItem> todaySchedules =
                ScheduleRepository.getSchedulesByDate(this, today);
        boolean updated = false;
        for (ScheduleMedicineItem item : todaySchedules) {
            if (item == null) continue;
            if (timeSlot.equals(item.getTimeSlot()) && !item.isCompletedOn(today)) {
                item.setCompletedOn(today, true);
                ScheduleRepository.updateCompletedForDate(this, item, today);
                updated = true;
            }
        }
        if (updated) {
            renderScheduleForSelectedDate();
            Toast.makeText(this,
                    timeSlot + " 복약 완료 처리되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(selectedCalendar.getTime());
    }

    private boolean isToday(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH);
    }
}
