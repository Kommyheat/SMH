package com.app.smh;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.app.smh.auth.AuthApiClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

public class ResetPasswordActivity extends AppCompatActivity {

    private AuthApiClient authApiClient;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        authApiClient = new AuthApiClient();

        ImageButton btnBack = findViewById(R.id.btn_back_reset_password);
        EditText etNewPassword = findViewById(R.id.et_new_password);
        EditText etNewPasswordConfirm = findViewById(R.id.et_new_password_confirm);
        Button btnSubmit = findViewById(R.id.btn_reset_password_submit);
        TextView tvPasswordWarning = findViewById(R.id.tv_reset_password_warning);
        TextView tvPasswordConfirmWarning = findViewById(R.id.tv_reset_password_confirm_warning);

        String email = getIntent().getStringExtra("email");
        String verifyToken = getIntent().getStringExtra("verifyToken");

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = etNewPassword != null ? etNewPassword.getText().toString().trim() : "";
                String confirm = etNewPasswordConfirm != null ? etNewPasswordConfirm.getText().toString().trim() : "";

                boolean valid = isValidPassword(password);
                boolean matched = !confirm.isEmpty() && password.equals(confirm);

                if (tvPasswordWarning != null) {
                    if (!password.isEmpty() && !valid) {
                        tvPasswordWarning.setText("비밀번호는 8자 이상, 영문/숫자/특수기호를 모두 포함해야 합니다.");
                        tvPasswordWarning.setVisibility(TextView.VISIBLE);
                    } else {
                        tvPasswordWarning.setText("");
                        tvPasswordWarning.setVisibility(TextView.GONE);
                    }
                }

                if (tvPasswordConfirmWarning != null) {
                    if (!confirm.isEmpty() && !matched) {
                        tvPasswordConfirmWarning.setText("비밀번호 확인이 일치하지 않습니다.");
                        tvPasswordConfirmWarning.setVisibility(TextView.VISIBLE);
                    } else {
                        tvPasswordConfirmWarning.setText("");
                        tvPasswordConfirmWarning.setVisibility(TextView.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        if (etNewPassword != null) etNewPassword.addTextChangedListener(watcher);
        if (etNewPasswordConfirm != null) etNewPasswordConfirm.addTextChangedListener(watcher);
        setupPasswordToggle(etNewPassword, R.id.et_new_password);
        setupPasswordToggle(etNewPasswordConfirm, R.id.et_new_password_confirm);

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String password = etNewPassword != null ? etNewPassword.getText().toString().trim() : "";
                String confirm = etNewPasswordConfirm != null ? etNewPasswordConfirm.getText().toString().trim() : "";

                if (email == null || email.trim().isEmpty() || verifyToken == null || verifyToken.trim().isEmpty()) {
                    Toast.makeText(this, "인증 정보가 유효하지 않습니다.\n다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidPassword(password)) {
                    Toast.makeText(this, "비밀번호 조건을 확인해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.equals(confirm)) {
                    Toast.makeText(this, "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                showLoadingDialog("비밀번호를 변경중입니다...");
                new Thread(() -> {
                    try {
                        AuthApiClient.ResetPasswordByCodeRequest request = new AuthApiClient.ResetPasswordByCodeRequest();
                        request.email = email;
                        request.verifyToken = verifyToken;
                        request.newPassword = password;

                        AuthApiClient.MessageResponse response = authApiClient.resetPasswordByCode(request);
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        });
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        });
                    } catch (AuthApiClient.ApiException e) {
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            });
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");
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

    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pbParams.rightMargin = (int) (12 * getResources().getDisplayMetrics().density);
        progressBar.setLayoutParams(pbParams);

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(15f);
        textView.setGravity(Gravity.CENTER_VERTICAL);

        container.addView(progressBar);
        container.addView(textView);

        loadingDialog = new MaterialAlertDialogBuilder(this)
                .setView(container)
                .setCancelable(false)
                .create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        hideLoadingDialog();
        super.onDestroy();
    }
}
