package com.app.smh;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import java.io.File;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.app.smh.auth.AuthApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class MyPageActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private LinearLayout layoutHomeMessage;
    private LinearLayout layoutAlarm;
    private LinearLayout layoutGuardian;
    private LinearLayout layoutContact;
    private LinearLayout layoutLogin;
    private LinearLayout layoutLinkCode;

    private Switch switchDarkMode;
    private Switch switchTts;
    private TextView tvLoginRequired;
    private TextView tvLoginOutTitle;
    private TextView tvLinkCode;
    private ImageButton btnCopyLinkCode;

    private AuthApiClient authApiClient;
    private String currentLinkCode = "";

    private androidx.appcompat.widget.SwitchCompat switchSeniorMode;
    private LinearLayout layoutSeniorModeSwitch;
    private LinearLayout layoutSeniorMode;
    private LinearLayout layoutTts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);
        authApiClient = new AuthApiClient();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        initViews();
        setupBottomNavigation();
        setupClickListeners();
        setupSwitchListeners();
        updateHomeMessagePreview();
        updateLoginUiState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHomeMessagePreview();
        updateLoginUiState();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_nav_profile);

        layoutHomeMessage = findViewById(R.id.layout_home_message);
        layoutAlarm = findViewById(R.id.layout_alarm);
        layoutGuardian = findViewById(R.id.layout_guardian);
        layoutContact = findViewById(R.id.layout_contact);
        layoutLogin = findViewById(R.id.layout_login);
        layoutLinkCode = findViewById(R.id.layout_link_code);

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchTts = findViewById(R.id.switch_tts);

// 추가
        switchSeniorMode = findViewById(R.id.switch_senior_mode);
        layoutSeniorMode = findViewById(R.id.layout_senior_mode);
        layoutTts = findViewById(R.id.layout_tts);

        switchDarkMode.setChecked(SettingsManager.isDarkModeEnabled(this));
        switchTts.setChecked(SettingsManager.isTtsEnabled(this));

        switchSeniorMode.setChecked(SettingsManager.isSeniorModeEnabled(this));
        applySeniorModeUi(SettingsManager.isSeniorModeEnabled(this));

        tvLoginRequired = findViewById(R.id.tv_login_required);
        tvLoginOutTitle = findViewById(R.id.tv_login_out_title);
        tvLinkCode = findViewById(R.id.tv_link_code);
        btnCopyLinkCode = findViewById(R.id.btn_copy_link_code);

        switchDarkMode.setChecked(SettingsManager.isDarkModeEnabled(this));
        switchTts.setChecked(SettingsManager.isTtsEnabled(this));

        if (btnCopyLinkCode != null) {
            btnCopyLinkCode.setOnClickListener(v -> copyLinkCode());
        }
    }

    private void updateLoginUiState() {
        boolean loggedIn = SettingsManager.isLoggedIn(this);

        // 프로필 이미지 적용
        ImageView ivProfile = findViewById(R.id.iv_profile);
        if (ivProfile != null) {
            String savedPath = SettingsManager.getProfileImagePath(this);
            if (savedPath != null) {
                File file = new File(savedPath);
                if (file.exists()) {
                    ivProfile.setImageURI(null);
                    ivProfile.setImageURI(Uri.fromFile(file));
                    ivProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivProfile.setClipToOutline(true);
                    ivProfile.setBackgroundResource(R.drawable.bg_profile_circle);
                } else {
                    // 파일 없으면 기본 아이콘
                    ivProfile.setImageResource(R.drawable.ic_user);
                    ivProfile.setBackground(null);
                }
            } else {
                // 경로 없으면 기본 아이콘
                ivProfile.setImageResource(R.drawable.ic_user);
                ivProfile.setBackground(null);
            }
        }

        if (tvLoginRequired != null) {
            if (loggedIn) {
                String name = SettingsManager.getLoginUserName(this);
                if (name == null || name.trim().isEmpty()) {
                    tvLoginRequired.setText("로그인 완료");
                } else {
                    tvLoginRequired.setText(buildGreetingText(name));
                }
                if (layoutLinkCode != null) layoutLinkCode.setVisibility(View.VISIBLE);
                if (btnCopyLinkCode != null) btnCopyLinkCode.setVisibility(View.VISIBLE);
                fetchAndShowLinkCode();
            } else {
                tvLoginRequired.setText("로그인이 필요합니다.");
                currentLinkCode = "";
                if (layoutLinkCode != null) layoutLinkCode.setVisibility(View.GONE);
                if (btnCopyLinkCode != null) btnCopyLinkCode.setVisibility(View.GONE);
            }
        }
        if (tvLoginOutTitle != null) {
            tvLoginOutTitle.setText(loggedIn ? "로그아웃" : "로그인");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(MyPageActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_scan) {
                Intent intent = new Intent(MyPageActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("open_tab", "scan");
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        LinearLayout layoutLoginRequired = findViewById(R.id.layout_login_required);
        if (layoutLoginRequired != null) {
            layoutLoginRequired.setOnClickListener(v -> {
                if (SettingsManager.isLoggedIn(this)) {
                    startActivity(new Intent(MyPageActivity.this, UserProfileActivity.class));
                } else {
                    startActivity(new Intent(MyPageActivity.this, LoginActivity.class));
                }
            });
        }

        layoutHomeMessage.setOnClickListener(v -> showHomeMessageDialog());

        layoutAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, AlarmSettingsActivity.class);
            startActivity(intent);
        });


        if (layoutSeniorMode != null) {
            layoutSeniorMode.setOnClickListener(v ->
                    startActivity(new Intent(MyPageActivity.this, FontSizeActivity.class))
            );
        }

        layoutGuardian.setOnClickListener(v -> {
            startActivity(new Intent(MyPageActivity.this, GuardianLinkActivity.class));
        });

        layoutContact.setOnClickListener(v ->
                startActivity(new Intent(MyPageActivity.this, InquiryActivity.class))
        );

        layoutLogin.setOnClickListener(v -> {
            if (SettingsManager.isLoggedIn(this)) {
                SettingsManager.clearDeveloperAutoLogin(this);
                SettingsManager.clearLoginSession(this);
                Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MyPageActivity.this, LoginActivity.class));
                finish();
            } else {
                startActivity(new Intent(MyPageActivity.this, LoginActivity.class));
            }
        });
    }


    private void setupSwitchListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setDarkModeEnabled(this, isChecked);

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            Toast.makeText(this,
                    isChecked ? "다크 모드가 활성화 되었습니다." : "다크 모드가 비활성화 되었습니다.",
                    Toast.LENGTH_SHORT).show();
        });

        switchTts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setTtsEnabled(this, isChecked);
            Toast.makeText(this, isChecked ? "TTS 설정이 활성화 되었습니다." : "TTS 설정이 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
        });

        // 고령자 모드 스위치 리스너
        if (switchSeniorMode != null) {
            switchSeniorMode.setOnCheckedChangeListener((btn, isChecked) -> {
                SettingsManager.setSeniorModeEnabled(this, isChecked);

                if (isChecked) {
                    // TTS 자동 켜기
                    SettingsManager.setTtsEnabled(this, true);
                    switchTts.setChecked(true);

                    // 최대 글자 크기 적용
                    SettingsManager.setFontScale(this, 1.5f);
                    android.content.res.Configuration config =
                            new android.content.res.Configuration(
                                    getResources().getConfiguration());
                    config.fontScale = 1.5f;
                    getResources().updateConfiguration(
                            config, getResources().getDisplayMetrics());

                    Toast.makeText(this,
                            "고령자 모드가 활성화되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "고령자 모드가 비활성화되었습니다.", Toast.LENGTH_SHORT).show();
                }

                applySeniorModeUi(isChecked);
            });
        }
    }

    // 고령자 모드 UI 적용 메서드
    private void applySeniorModeUi(boolean seniorModeEnabled) {
        float alpha = seniorModeEnabled ? 0.4f : 1.0f;
        boolean enabled = !seniorModeEnabled;

        // 글자 크기 항목 비활성화/활성화
        if (layoutSeniorMode != null) {
            layoutSeniorMode.setAlpha(alpha);
            layoutSeniorMode.setEnabled(enabled);
            layoutSeniorMode.setClickable(enabled);
        }

        // TTS 스위치 비활성화/활성화
        if (switchTts != null) {
            switchTts.setEnabled(enabled);
        }
        if (layoutTts != null) {
            layoutTts.setAlpha(alpha);
        }
    }


    private void showHomeMessageDialog() {
        final EditText editText = new EditText(this);
        editText.setText(SettingsManager.getHomeMessage(this));
        editText.setSelection(editText.getText().length());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        editText.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle("홈 문구 설정")
                .setMessage("메인 화면 배너에 표시할 문구를 입력하세요.")
                .setView(editText)
                .setPositiveButton("저장", (dialog, which) -> {
                    String input = editText.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "문구를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SettingsManager.saveHomeMessage(this, input);
                    updateHomeMessagePreview();
                    Toast.makeText(this, "홈 문구가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateHomeMessagePreview() {
        TextView tvHomeMessageDesc = findViewById(R.id.tv_home_message_desc);
        tvHomeMessageDesc.setText(SettingsManager.getHomeMessage(this));
    }

    private void fetchAndShowLinkCode() {
        long userId = SettingsManager.getLoginUserId(this);
        if (userId <= 0) return;

        if (tvLinkCode != null) {
            tvLinkCode.setText("내 코드 불러오는 중...");
        }

        new Thread(() -> {
            try {
                AuthApiClient.LinkCodeResponse response = authApiClient.getMyLinkCode(userId);
                runOnUiThread(() -> {
                    currentLinkCode = response != null && response.linkCode != null ? response.linkCode : "";
                    if (tvLinkCode != null) {
                        if (currentLinkCode.isEmpty()) {
                            tvLinkCode.setText("내 코드: -");
                        } else {
                            tvLinkCode.setText(buildLinkCodeText(currentLinkCode));
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    currentLinkCode = "";
                    if (tvLinkCode != null) {
                        tvLinkCode.setText("내 코드: -");
                    }
                });
            }
        }).start();
    }

    private void copyLinkCode() {
        if (currentLinkCode == null || currentLinkCode.trim().isEmpty()) {
            Toast.makeText(this, "복사할 코드가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("내 코드", currentLinkCode));
            Toast.makeText(this, "코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private CharSequence buildGreetingText(String name) {
        String full = name + "님, 안녕하세요.";
        SpannableString span = new SpannableString(full);
        int coral = ContextCompat.getColor(this, R.color.main_coral);
        span.setSpan(new ForegroundColorSpan(coral), 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    private CharSequence buildLinkCodeText(String linkCode) {
        String prefix = "내 코드: ";
        String full = prefix + linkCode;
        SpannableString span = new SpannableString(full);
        int coral = ContextCompat.getColor(this, R.color.main_coral);
        span.setSpan(new ForegroundColorSpan(coral), prefix.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }
}
