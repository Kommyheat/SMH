package com.app.smh;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.util.Patterns;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import com.app.smh.auth.AuthApiClient;

import java.io.IOException;

public class SignUpActivity extends AppCompatActivity {

    private View btnCloseLogin;

    private EditText etSignUpName;
    private EditText etSignUpBirth;
    private EditText etSignUpEmail;
    private EditText etSignUpId;
    private EditText etSignUpPassword;
    private EditText etSignUpPasswordConfirm;

    private Button btnCheckIdDuplicate;
    private Button btnSignUpComplete;

    private TextView tvIdCheckMessage;
    private TextView tvPasswordWarning;
    private TextView tvPasswordConfirmWarning;

    private AuthApiClient authApiClient;
    private boolean isLoginIdChecked = false;
    private boolean isFormattingBirthDate = false;

    private final TextWatcher signUpTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            validateSignUpForm();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        authApiClient = new AuthApiClient();

        initViews();
        setupListeners();
        applySocialPrefillIfExists();
        validateSignUpForm();
    }

    private void initViews() {
        btnCloseLogin = findViewById(R.id.btn_close_login);

        etSignUpName = findViewById(R.id.et_sign_up_name);
        etSignUpBirth = findViewById(R.id.et_sign_up_birth);
        etSignUpEmail = findViewById(R.id.et_sign_up_email);
        etSignUpId = findViewById(R.id.et_sign_up_id);
        etSignUpPassword = findViewById(R.id.et_sign_up_password);
        etSignUpPasswordConfirm = findViewById(R.id.et_sign_up_password_confirm);

        btnCheckIdDuplicate = findViewById(R.id.btn_check_id_duplicate);
        btnSignUpComplete = findViewById(R.id.btn_sign_up_complete);

        tvIdCheckMessage = findViewById(R.id.tv_id_check_message);
        tvPasswordWarning = findViewById(R.id.tv_password_warning);
        tvPasswordConfirmWarning = findViewById(R.id.tv_password_confirm_warning);
    }

    private void setupListeners() {
        if (btnCloseLogin != null) {
            btnCloseLogin.setOnClickListener(v -> finish());
        }

        addWatcher(etSignUpName);
        addWatcher(etSignUpBirth);
        addWatcher(etSignUpEmail);
        addWatcher(etSignUpId);
        addWatcher(etSignUpPassword);
        addWatcher(etSignUpPasswordConfirm);

        if (etSignUpId != null) {
            etSignUpId.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged() {
                    isLoginIdChecked = false;
                    if (tvIdCheckMessage != null) {
                        tvIdCheckMessage.setVisibility(View.GONE);
                        tvIdCheckMessage.setText("");
                    }
                    validateSignUpForm();
                }
            });
        }

        if (etSignUpBirth != null) {
            etSignUpBirth.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            etSignUpBirth.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    formatBirthDateInput(s);
                }
            });
        }

        if (btnCheckIdDuplicate != null) {
            btnCheckIdDuplicate.setOnClickListener(v -> checkIdDuplicate());
        }

        if (btnSignUpComplete != null) {
            btnSignUpComplete.setOnClickListener(v -> completeSignUp());
        }

        setupPasswordToggle(etSignUpPassword, R.id.et_sign_up_password);
        setupPasswordToggle(etSignUpPasswordConfirm, R.id.et_sign_up_password_confirm);
    }

    private void addWatcher(EditText editText) {
        if (editText != null) {
            editText.addTextChangedListener(signUpTextWatcher);
        }
    }

    private void applySocialPrefillIfExists() {
        String socialProvider = getIntent().getStringExtra("social_provider");
        if (socialProvider != null && !socialProvider.trim().isEmpty()) {
            Toast.makeText(this, socialProvider + " 계정 연동은 추후 지원됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateSignUpForm() {
        String name = getTrimmedText(etSignUpName);
        String birth = getTrimmedText(etSignUpBirth);
        String email = getTrimmedText(etSignUpEmail);
        String id = getTrimmedText(etSignUpId);
        String password = getTrimmedText(etSignUpPassword);
        String passwordConfirm = getTrimmedText(etSignUpPasswordConfirm);

        boolean isPasswordValid = isValidPassword(password);
        boolean isPasswordMatch = !password.isEmpty() && password.equals(passwordConfirm);

        if (tvPasswordWarning != null) {
            if (!password.isEmpty() && !isPasswordValid) {
                tvPasswordWarning.setText("비밀번호는 8자 이상, 영문/숫자/특수기호를 모두 포함해야 합니다.");
                tvPasswordWarning.setVisibility(View.VISIBLE);
            } else {
                tvPasswordWarning.setText("");
                tvPasswordWarning.setVisibility(View.GONE);
            }
        }

        if (tvPasswordConfirmWarning != null) {
            if (!passwordConfirm.isEmpty() && !isPasswordMatch) {
                tvPasswordConfirmWarning.setText("비밀번호 확인이 일치하지 않습니다.");
                tvPasswordConfirmWarning.setVisibility(View.VISIBLE);
            } else {
                tvPasswordConfirmWarning.setText("");
                tvPasswordConfirmWarning.setVisibility(View.GONE);
            }
        }

        boolean allFilled = !name.isEmpty()
                && !birth.isEmpty()
                && !email.isEmpty()
                && !id.isEmpty()
                && !password.isEmpty()
                && !passwordConfirm.isEmpty();

        boolean canSubmit = allFilled && isPasswordValid && isPasswordMatch && isLoginIdChecked;

        if (btnSignUpComplete != null) {
            btnSignUpComplete.setEnabled(canSubmit);
            btnSignUpComplete.setAlpha(1f);
        }
    }

    private void checkIdDuplicate() {
        final String id = getTrimmedText(etSignUpId);
        if (id.isEmpty()) {
            if (tvIdCheckMessage != null) {
                tvIdCheckMessage.setText("아이디를 입력한 뒤 중복 확인을 진행해주세요.");
                tvIdCheckMessage.setVisibility(View.VISIBLE);
            }
            return;
        }

        new Thread(() -> {
            try {
                boolean isAvailable = authApiClient.checkLoginIdAvailable(id);
                runOnUiThread(() -> {
                    isLoginIdChecked = isAvailable;
                    if (tvIdCheckMessage != null) {
                        tvIdCheckMessage.setText(isAvailable
                                ? "사용 가능한 아이디입니다."
                                : "이미 사용 중인 아이디입니다.");
                        tvIdCheckMessage.setVisibility(View.VISIBLE);
                    }
                    validateSignUpForm();
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            } catch (AuthApiClient.ApiException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void completeSignUp() {
        String name = getTrimmedText(etSignUpName);
        String birth = getTrimmedText(etSignUpBirth);
        String email = getTrimmedText(etSignUpEmail);
        String id = getTrimmedText(etSignUpId);
        String password = getTrimmedText(etSignUpPassword);
        String passwordConfirm = getTrimmedText(etSignUpPasswordConfirm);

        if (name.isEmpty() || birth.isEmpty() || email.isEmpty() || id.isEmpty()
                || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "비밀번호 조건을 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidBirthDateFormat(birth)) {
            Toast.makeText(this, "생년월일은 yyyy-MM-dd 형식으로 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmailFormat(email)) {
            Toast.makeText(this, "이메일 형식을 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLoginIdChecked) {
            Toast.makeText(this, "아이디 중복 확인을 먼저 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthApiClient.SignupRequest request = new AuthApiClient.SignupRequest();
        request.loginId = id;
        request.password = password;
        request.passwordOk = passwordConfirm;
        request.name = name;
        request.email = email;
        request.birthDate = birth;

        new Thread(() -> {
            try {
                AuthApiClient.MessageResponse response = authApiClient.signup(request);
                runOnUiThread(() -> {
                    Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            } catch (AuthApiClient.ApiException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");
    }

    private boolean isValidBirthDateFormat(String birthDate) {
        if (birthDate == null) return false;
        return birthDate.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    private boolean isValidEmailFormat(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void formatBirthDateInput(Editable editable) {
        if (isFormattingBirthDate || editable == null) return;

        isFormattingBirthDate = true;
        String digits = editable.toString().replaceAll("[^\\d]", "");
        if (digits.length() > 8) {
            digits = digits.substring(0, 8);
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 4 || i == 6) {
                formatted.append('-');
            }
            formatted.append(digits.charAt(i));
        }

        String result = formatted.toString();
        if (!result.equals(editable.toString())) {
            editable.replace(0, editable.length(), result);
        }
        isFormattingBirthDate = false;
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

    private void setupPasswordToggle(EditText editText, int tagKey) {
        if (editText == null) return;
        editText.setTag(tagKey, false);
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.baseline_visibility_off_24), null
        );
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            if (editText.getCompoundDrawablesRelative()[2] == null) return false;

            int drawableWidth = editText.getCompoundDrawablesRelative()[2].getBounds().width();
            boolean tapped = event.getX() >= (editText.getWidth() - editText.getPaddingEnd() - drawableWidth);
            if (!tapped) return false;

            boolean visible = Boolean.TRUE.equals(editText.getTag(tagKey));
            boolean nextVisible = !visible;
            setPasswordVisible(editText, nextVisible);
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(this, nextVisible ? R.drawable.baseline_visibility_24 : R.drawable.baseline_visibility_off_24),
                    null
            );
            editText.setTag(tagKey, nextVisible);
            return true;
        });
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChanged();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

        public abstract void onChanged();
    }
}
