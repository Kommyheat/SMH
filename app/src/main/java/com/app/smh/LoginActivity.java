package com.app.smh;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.app.smh.auth.AuthApiClient;
import com.app.smh.schedule.MedicationServerSync;
import com.app.smh.schedule.ScheduleRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private ImageButton btnCloseLogin;
    private EditText etLoginId;
    private EditText etLoginPassword;
    private CheckBox cbAutoLogin;
    private Button btnLogin;
    private TextView tvFindId;
    private TextView tvFindPassword;
    private TextView tvGoSignUp;
    private ImageButton btnGoogleLogin;
    private ImageButton btnKakaoLogin;
    private ImageButton btnNaverLogin;

    private AuthApiClient authApiClient;

    private final TextWatcher loginTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateLoginButtonState();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        authApiClient = new AuthApiClient();

        initViews();
        setupListeners();
        preloadAutoLoginInputs();
        updateLoginButtonState();
    }

    private void initViews() {
        btnCloseLogin = findViewById(R.id.btn_close_login);
        etLoginId = findViewById(R.id.et_login_id);
        etLoginPassword = findViewById(R.id.et_login_password);
        cbAutoLogin = findViewById(R.id.cb_auto_login);
        btnLogin = findViewById(R.id.btn_login);
        tvFindId = findViewById(R.id.tv_find_id);
        tvFindPassword = findViewById(R.id.tv_find_password);
        tvGoSignUp = findViewById(R.id.tv_go_sign_up);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        btnKakaoLogin = findViewById(R.id.btn_kakao_login);
        btnNaverLogin = findViewById(R.id.btn_naver_login);
    }

    private void setupListeners() {
        if (btnCloseLogin != null) btnCloseLogin.setOnClickListener(v -> finish());
        if (etLoginId != null) etLoginId.addTextChangedListener(loginTextWatcher);
        if (etLoginPassword != null) etLoginPassword.addTextChangedListener(loginTextWatcher);
        if (btnLogin != null) btnLogin.setOnClickListener(v -> attemptLogin());
        setupPasswordToggle(etLoginPassword);

        if (tvGoSignUp != null) {
            tvGoSignUp.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
        }
        if (tvFindId != null) tvFindId.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, FindIdActivity.class)));
        if (tvFindPassword != null) tvFindPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, FindPasswordActivity.class)));
        if (btnGoogleLogin != null) btnGoogleLogin.setOnClickListener(v -> handleSocialLogin("Google"));
        if (btnKakaoLogin != null) btnKakaoLogin.setOnClickListener(v -> handleSocialLogin("Kakao"));
        if (btnNaverLogin != null) btnNaverLogin.setOnClickListener(v -> handleSocialLogin("Naver"));
    }

    private void updateLoginButtonState() {
        if (btnLogin == null) return;

        String id = getTrimmedText(etLoginId);
        String password = getTrimmedText(etLoginPassword);

        boolean isFilled = !id.isEmpty() && !password.isEmpty();

        btnLogin.setEnabled(isFilled);
    }

    private void attemptLogin() {
        String id = getTrimmedText(etLoginId);
        String password = getTrimmedText(etLoginPassword);
        boolean autoLoginChecked = cbAutoLogin != null && cbAutoLogin.isChecked();

        if (id.isEmpty()) {
            etLoginId.setError("아이디를 입력해주세요.");
            etLoginId.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etLoginPassword.setError("비밀번호를 입력해주세요.");
            etLoginPassword.requestFocus();
            return;
        }

        AuthApiClient.LoginRequest request = new AuthApiClient.LoginRequest();
        request.loginId = id;
        request.password = password;

        new Thread(() -> {
            try {
                AuthApiClient.LoginResponse response = authApiClient.login(request);
                runOnUiThread(() -> {
                    // 로그인 세션 저장 (기존 유지)
                    SettingsManager.saveLoginSession(this, response.id, response.loginId, response.name);

                    if (response.birthDate != null) {
                        SettingsManager.setBirthDate(this, response.birthDate);
                    }
                    if (response.email != null) {
                        SettingsManager.setEmail(this, response.email);
                    }
                    // 자동 로그인 체크 여부 처리 (기존 유지)
                    if (autoLoginChecked) {
                        SettingsManager.saveAutoLoginCredentials(this, id, password);
                    } else {
                        SettingsManager.clearAutoLoginCredentials(this);
                    }

                    Toast.makeText(this, response.name + "님, 환영합니다.", Toast.LENGTH_SHORT).show();

                    // 서버에서 복약 스케줄 불러와서 로컬 동기화 후 메인으로 이동
                    MedicationServerSync.syncFromServer(this, () ->
                            runOnUiThread(() -> {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            })
                    );
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            } catch (AuthApiClient.ApiException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void preloadAutoLoginInputs() {
        if (etLoginId == null || etLoginPassword == null || cbAutoLogin == null) return;
        if (!SettingsManager.hasAutoLoginCredentials(this)) return;

        etLoginId.setText(SettingsManager.getAutoLoginId(this));
        etLoginPassword.setText(SettingsManager.getAutoLoginPassword(this));
        cbAutoLogin.setChecked(true);
    }

    private LinearLayout createDialogLayout(EditText... inputs) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        layout.setPadding(padding, padding, padding, padding / 2);
        for (EditText input : inputs) layout.addView(input);
        return layout;
    }

    private EditText createDialogInput(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dpToPx(8);
        editText.setLayoutParams(params);
        return editText;
    }

    private void applyBirthDateAutoFormat(EditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789-"));
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting || s == null) return;
                isFormatting = true;

                String digits = s.toString().replaceAll("[^\\d]", "");
                if (digits.length() > 8) digits = digits.substring(0, 8);

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i == 4 || i == 6) formatted.append('-');
                    formatted.append(digits.charAt(i));
                }

                String result = formatted.toString();
                if (!result.equals(s.toString())) {
                    s.replace(0, s.length(), result);
                    editText.setSelection(result.length());
                }
                isFormatting = false;
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }

    private boolean isValidBirthDateFormat(String birthDate) {
        if (birthDate == null) return false;
        return birthDate.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    private boolean isValidEmailFormat(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void handleSocialLogin(String provider) {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        intent.putExtra("social_provider", provider);
        startActivity(intent);
    }

    private String getTrimmedText(EditText editText) {
        return editText != null ? editText.getText().toString().trim() : "";
    }

    private void setPasswordVisible(EditText editText, boolean visible) {
        if (editText == null) return;
        int selection = editText.getSelectionEnd();
        editText.setTransformationMethod(visible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        if (selection >= 0 && selection <= editText.length()) {
            editText.setSelection(selection);
        }
    }

    private void setupPasswordToggle(EditText editText) {
        if (editText == null) return;
        editText.setTag(R.id.et_login_password, false);
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.baseline_visibility_off_24), null
        );
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            if (editText.getCompoundDrawablesRelative()[2] == null) return false;

            int drawableWidth = editText.getCompoundDrawablesRelative()[2].getBounds().width();
            boolean tapped = event.getX() >= (editText.getWidth() - editText.getPaddingEnd() - drawableWidth);
            if (!tapped) return false;

            boolean visible = Boolean.TRUE.equals(editText.getTag(R.id.et_login_password));
            boolean nextVisible = !visible;
            setPasswordVisible(editText, nextVisible);
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(this, nextVisible ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24),
                    null
            );
            editText.setTag(R.id.et_login_password, nextVisible);
            return true;
        });
    }

    private void copyTemporaryPassword(String temporaryPassword) {
        if (temporaryPassword == null || temporaryPassword.trim().isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;

        ClipData clipData = ClipData.newPlainText("임시 비밀번호", temporaryPassword);
        clipboard.setPrimaryClip(clipData);
    }
}
