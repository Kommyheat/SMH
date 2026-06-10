package com.app.smh;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.inputmethod.InputMethodManager;
import android.view.Gravity;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.app.smh.auth.AuthApiClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

public class FindIdActivity extends AppCompatActivity {

    private AuthApiClient authApiClient;
    private CountDownTimer codeTimer;
    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private AlertDialog loadingDialog;
    private Button btnSendCode;
    private Button btnVerifyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);
        authApiClient = new AuthApiClient();

        ImageButton btnBack = findViewById(R.id.btn_back_find_id);
        EditText etEmail = findViewById(R.id.et_find_id_email);
        EditText etCode = findViewById(R.id.et_find_id_code);
        btnSendCode = findViewById(R.id.btn_find_id_send_code);
        btnVerifyCode = findViewById(R.id.btn_find_id_verify_code);
        TextView tvResult = findViewById(R.id.tv_find_id_result);
        TextView tvCodeTimer = findViewById(R.id.tv_find_id_code_timer);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        setVerifyButtonEnabled(false);

        if (btnSendCode != null) {
            btnSendCode.setOnClickListener(v -> {
                String email = etEmail != null ? etEmail.getText().toString().trim() : "";
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "이메일 형식을 확인해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    try {
                        runOnUiThread(this::showLoadingDialog);
                        AuthApiClient.EmailCodeSendRequest request = new AuthApiClient.EmailCodeSendRequest();
                        request.email = email;
                        request.purpose = "FIND_ID";
                        AuthApiClient.MessageResponse response = authApiClient.sendEmailCode(request);
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show();
                            if (etCode != null) {
                                etCode.requestFocus();
                                etCode.setSelection(etCode.getText().length());
                            }
                            setVerifyButtonEnabled(true);
                            startCodeTimer(tvCodeTimer);
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

        if (btnVerifyCode != null) {
            btnVerifyCode.setOnClickListener(v -> {
                hideKeyboard();
                String email = etEmail != null ? etEmail.getText().toString().trim() : "";
                String code = etCode != null ? etCode.getText().toString().trim() : "";
                if (email.isEmpty() || code.isEmpty()) {
                    Toast.makeText(this, "이메일과 인증코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    try {
                        AuthApiClient.EmailCodeVerifyRequest request = new AuthApiClient.EmailCodeVerifyRequest();
                        request.email = email;
                        request.purpose = "FIND_ID";
                        request.code = code;

                        AuthApiClient.EmailCodeVerifyResponse response = authApiClient.verifyEmailCode(request);
                        runOnUiThread(() -> {
                            if (response == null || !response.verified || response.verifyToken == null) {
                                Toast.makeText(this, "인증에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new Thread(() -> {
                                try {
                                    AuthApiClient.FindLoginIdByCodeRequest findRequest = new AuthApiClient.FindLoginIdByCodeRequest();
                                    findRequest.email = email;
                                    findRequest.verifyToken = response.verifyToken;

                                    AuthApiClient.FindLoginIdResponse findResponse = authApiClient.findLoginIdByCode(findRequest);
                                    runOnUiThread(() -> {
                                        if (tvResult != null) tvResult.setVisibility(View.GONE);
                                        showLoginIdResultDialog(findResponse.loginId);
                                        if (codeTimer != null) codeTimer.cancel();
                                        if (tvCodeTimer != null) tvCodeTimer.setVisibility(View.GONE);
                                        setVerifyButtonEnabled(false);
                                    });
                                } catch (IOException e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                    });
                                } catch (AuthApiClient.ApiException e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }).start();
                        });
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        });
                    } catch (AuthApiClient.ApiException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            });
        }
    }

    private void startCodeTimer(TextView timerView) {
        if (timerView == null) return;
        if (codeTimer != null) codeTimer.cancel();
        if (btnSendCode != null) btnSendCode.setEnabled(false);

        timerView.setVisibility(View.VISIBLE);
        codeTimer = new CountDownTimer(CODE_EXPIRE_MILLIS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSec = millisUntilFinished / 1000L;
                long min = totalSec / 60L;
                long sec = totalSec % 60L;
                timerView.setText(String.format("남은 시간 %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                timerView.setText("인증코드가 만료되었습니다.\n다시 발송해주세요.");
                if (btnSendCode != null) btnSendCode.setEnabled(true);
                setVerifyButtonEnabled(false);
            }
        };
        codeTimer.start();
    }

    private void showLoadingDialog() {
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
        textView.setText("이메일을 전송 중입니다...");
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

    private void setVerifyButtonEnabled(boolean enabled) {
        if (btnVerifyCode == null) return;
        btnVerifyCode.setEnabled(enabled);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void showLoginIdResultDialog(String loginId) {
        String safeLoginId = loginId == null ? "" : loginId;
        new MaterialAlertDialogBuilder(this)
                .setTitle("아이디 정보")
                .setMessage("귀하의 아이디는 \"" + safeLoginId + "\" 입니다.")
                .setNegativeButton("닫기", null)
                .setPositiveButton("복사", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("아이디", safeLoginId));
                        Toast.makeText(this, "아이디가 복사되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        hideLoadingDialog();
        if (codeTimer != null) codeTimer.cancel();
        super.onDestroy();
    }
}
