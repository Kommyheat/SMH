package com.app.smh;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.app.smh.schedule.IntakeServerSync;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.smh.alarm.MedicationAlarmReceiver;
import com.app.smh.calendar.MedicationCalendarActivity;
import com.app.smh.health.TodayHealthManager;
import com.app.smh.scan.ScanCameraFragment;

import com.app.smh.schedule.ManualRegisterDialogFragment;
import com.app.smh.schedule.MedicationServerSync;
import com.app.smh.schedule.ScheduleMedicineItem;
import com.app.smh.schedule.ScheduleRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

    // 오늘의 건강 패널 관련 필드
    private View dimOverlay;
    private View todayHealthPanel;
    private boolean isHealthPanelVisible = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 13 이상에서만 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        initViews();
        setupBottomNavigation();
        setupFabChatbot();
        setupCalendarButton();
        setupHealthButton();
        applyHomeBannerMessage();
        setupDateNavigation();
        setupManualRegisterButton();
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

        // 오늘의 건강 패널
        dimOverlay = findViewById(R.id.dim_overlay);
        todayHealthPanel = findViewById(R.id.layout_today_health_panel);
    }

    // 하단 네비게이션
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

    // FAB / 캘린더 버튼
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

    // 오늘의 건강 패널
    private void setupHealthButton() {
        ImageView ivFavorite = findViewById(R.id.iv_favorite);
        if (ivFavorite == null) return;

        ivFavorite.setOnClickListener(v -> {
            if (isHealthPanelVisible) {
                hideHealthPanel(ivFavorite);
            } else {
                showHealthPanel(ivFavorite);
            }
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

    //  배너 / 날짜 네비게이션
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

        // 알림 클릭으로 진입한 경우 → 시간대 완료 처리
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

    //  복약 스케줄 렌더링
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

        // 현재 선택된 날짜 기준으로 완료 상태 판단
        String currentDate = getSelectedDateString();

        for (ScheduleMedicineItem item : items) {
            View itemView = inflater.inflate(R.layout.item_schedule_medicine, container, false);

            TextView tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            TextView btnTakeDone = itemView.findViewById(R.id.btn_take_done);
            View foreground = itemView.findViewById(R.id.layout_swipe_foreground);
            View btnDelete = itemView.findViewById(R.id.btn_delete_schedule);

            if (tvCategoryName != null) {
                tvCategoryName.setText(item.getCategoryName());
            }

            // 선택된 날짜 기준으로 완료 상태 확인
            boolean doneToday = item.isCompletedOn(currentDate);
            updateTakeButtonStyle(foreground, btnTakeDone, doneToday);

            if (btnTakeDone != null) {
                btnTakeDone.setOnClickListener(v -> {
                    boolean current = item.isCompletedOn(currentDate);
                    item.setCompletedOn(currentDate, !current);
                    ScheduleRepository.updateCompletedForDate(this, item, currentDate);
                    updateTakeButtonStyle(foreground, btnTakeDone, !current);

                    // 추가: 서버 intake_logs 동기화
                    if (!current) {
                        // 미완료 → 완료
                        IntakeServerSync.syncTaken(
                                this,
                                item.getCategoryName(),
                                item.getTimeSlot(),
                                currentDate
                        );
                    } else {
                        // 완료 → 미완료
                        IntakeServerSync.syncCanceled(
                                this,
                                item.getCategoryName(),
                                item.getTimeSlot(),
                                currentDate
                        );
                    }
                });
            }

            if (foreground != null) {
                setupSwipeToDelete(foreground, btnDelete, item);
            }

            container.addView(itemView);
        }
    }

    // boolean 파라미터로 받아서 날짜 기준 적용
    private void updateTakeButtonStyle(View foreground, TextView btnTakeDone, boolean isCompleted) {
        if (foreground == null || btnTakeDone == null) return;

        if (isCompleted) {
            foreground.setBackgroundResource(R.drawable.bg_schedule_item_done);
            btnTakeDone.setText("완료");
            btnTakeDone.setTextColor(ContextCompat.getColor(this, R.color.main_coral));
            btnTakeDone.setBackgroundResource(R.drawable.bg_schedule_done_button_done);
        } else {
            foreground.setBackgroundResource(R.drawable.bg_schedule_item_pending);
            btnTakeDone.setText("미완료");
            btnTakeDone.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
            btnTakeDone.setBackgroundResource(R.drawable.bg_schedule_done_button_pending);
        }
    }

    // 수기 등록 버튼
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
                    timeSlot + " 복약 완료 처리되었습니다.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeToDelete(View foreground, View deleteBtn, ScheduleMedicineItem item) {
        float deleteWidth = 80 * getResources().getDisplayMetrics().density;
        float threshold = deleteWidth / 2;

        final float[] startX = {0f};
        final float[] currentX = {0f};
        final boolean[] isOpen = {false};

        foreground.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = event.getRawX();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - startX[0];
                    float baseTx = isOpen[0] ? -deleteWidth : 0f;
                    float newTx = Math.min(0f, Math.max(-deleteWidth, baseTx + dx));
                    foreground.setTranslationX(newTx);
                    currentX[0] = newTx;
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float tx = currentX[0];
                    if (!isOpen[0]) {
                        if (tx < -threshold) {
                            openSwipe(foreground, deleteWidth);
                            isOpen[0] = true;
                        } else {
                            closeSwipe(foreground);
                            isOpen[0] = false;
                        }
                    } else {
                        if (tx > -threshold) {
                            closeSwipe(foreground);
                            isOpen[0] = false;
                        } else {
                            openSwipe(foreground, deleteWidth);
                            isOpen[0] = true;
                        }
                    }
                    return true;
            }
            return false;
        });

        // setupSwipeToDelete() 안의 삭제 버튼 클릭
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("복약 삭제")
                            .setMessage("'" + item.getCategoryName() + "' 복약 일정을 삭제할까요?")
                            .setNegativeButton("취소", (dialog, which) -> closeSwipe(foreground))
                            .setPositiveButton("삭제", (dialog, which) -> {
                                // 수정: 기존 로컬 삭제 → 서버+로컬 동시 삭제
                                MedicationServerSync.deleteMedication(
                                        this,
                                        item,
                                        () -> renderScheduleForSelectedDate()
                                );
                            })
                            .show()
            );
        }
    }

    private void openSwipe(View foreground, float deleteWidth) {
        foreground.animate()
                .translationX(-deleteWidth)
                .setDuration(150)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closeSwipe(View foreground) {
        foreground.animate()
                .translationX(0f)
                .setDuration(150)
                .setInterpolator(new DecelerateInterpolator())
                .start();
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
