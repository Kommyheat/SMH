package com.app.smh;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CancellationSignal;
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
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.app.smh.auth.AuthApiClient;
import com.app.smh.schedule.MedicationServerSync;
import com.app.smh.schedule.ScheduleRepository;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.common.model.ClientError;
import com.kakao.sdk.common.model.ClientErrorCause;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NidOAuth;
import com.navercorp.nid.oauth.util.NidOAuthCallback;

import java.io.IOException;
import java.util.concurrent.Executor;

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
    private CredentialManager credentialManager;
    private Executor mainExecutor;

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
        credentialManager = CredentialManager.create(this);
        mainExecutor = ContextCompat.getMainExecutor(this);
        KakaoSdk.init(getApplicationContext(), BuildConfig.KAKAO_NATIVE_APP_KEY);

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
        if (btnGoogleLogin != null) btnGoogleLogin.setOnClickListener(v -> attemptGoogleLogin());
        if (btnKakaoLogin != null) btnKakaoLogin.setOnClickListener(v -> attemptKakaoLogin());
        if (btnNaverLogin != null) btnNaverLogin.setOnClickListener(v -> attemptNaverLogin());
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
                runOnUiThread(() -> onLoginSuccess(response, autoLoginChecked, id, password));
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

    private void attemptNaverLogin() {
        NidOAuth.INSTANCE.initialize(
                this,
                BuildConfig.NAVER_CLIENT_ID,
                BuildConfig.NAVER_CLIENT_SECRET,
                BuildConfig.NAVER_CLIENT_NAME,
                null
        );

        NidOAuth.INSTANCE.requestLogin(this, new NidOAuthCallback() {
            @Override
            public void onSuccess() {
                String accessToken = NidOAuth.INSTANCE.getAccessToken();
                if (accessToken == null || accessToken.trim().isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "네이버 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show());
                    return;
                }
                loginWithNaverToken(accessToken);
            }

            @Override
            public void onFailure(String errorCode, String errorDesc) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "네이버 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void attemptKakaoLogin() {

        kotlin.jvm.functions.Function2<OAuthToken, Throwable, kotlin.Unit> accountCallback =
                new kotlin.jvm.functions.Function2<OAuthToken, Throwable, kotlin.Unit>() {
            @Override
            public kotlin.Unit invoke(OAuthToken token, Throwable error) {
                if (error != null) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "카카오 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show());
                } else if (token != null) {
                    loginWithKakaoToken(token.getAccessToken());
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "카카오 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show());
                }
                return kotlin.Unit.INSTANCE;
            }
        };

        if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
            UserApiClient.getInstance().loginWithKakaoTalk(this, (token, error) -> {
                if (error != null) {
                    if (error instanceof ClientError
                            && ((ClientError) error).getReason() == ClientErrorCause.Cancelled) {
                        return kotlin.Unit.INSTANCE;
                    }
                    UserApiClient.getInstance().loginWithKakaoAccount(LoginActivity.this, accountCallback);
                } else if (token != null) {
                    loginWithKakaoToken(token.getAccessToken());
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "카카오 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show());
                }
                return kotlin.Unit.INSTANCE;
            });
        } else {
            UserApiClient.getInstance().loginWithKakaoAccount(this, accountCallback);
        }
    }

    private void attemptGoogleLogin() {

        GetSignInWithGoogleOption googleIdOption = new GetSignInWithGoogleOption.Builder(
                BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CancellationSignal cancellationSignal = new CancellationSignal();
        credentialManager.getCredentialAsync(
                this,
                request,
                cancellationSignal,
                mainExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Toast.makeText(LoginActivity.this, "구글 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleGoogleCredential(Credential credential) {
        if (!(credential instanceof CustomCredential customCredential)) {
            Toast.makeText(this, "구글 로그인 정보 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
            Toast.makeText(this, "지원하지 않는 구글 로그인 타입입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
            String idToken = googleCredential.getIdToken();
            if (idToken.trim().isEmpty()) {
                Toast.makeText(this, "구글 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            loginWithGoogleToken(idToken);
        } catch (Exception e) {
            Toast.makeText(this, "구글 계정 정보를 해석하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithGoogleToken(String idToken) {
        new Thread(() -> {
            try {
                AuthApiClient.GoogleLoginRequest request = new AuthApiClient.GoogleLoginRequest();
                request.idToken = idToken;
                AuthApiClient.LoginResponse response = authApiClient.loginWithGoogle(request);
                runOnUiThread(() -> {
                    if (response.profileCompleted) {
                        onLoginSuccess(response, false, null, null);
                    } else {
                        showGoogleProfileCompletionDialog(response);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            } catch (AuthApiClient.ApiException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loginWithKakaoToken(String accessToken) {
        new Thread(() -> {
            try {
                AuthApiClient.KakaoLoginRequest request = new AuthApiClient.KakaoLoginRequest();
                request.accessToken = accessToken;
                AuthApiClient.LoginResponse response = authApiClient.loginWithKakao(request);
                runOnUiThread(() -> {
                    if (response.profileCompleted) {
                        onLoginSuccess(response, false, null, null);
                    } else {
                        showGoogleProfileCompletionDialog(response);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            } catch (AuthApiClient.ApiException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loginWithNaverToken(String accessToken) {
        new Thread(() -> {
            try {
                AuthApiClient.NaverLoginRequest request = new AuthApiClient.NaverLoginRequest();
                request.accessToken = accessToken;
                AuthApiClient.LoginResponse response = authApiClient.loginWithNaver(request);
                runOnUiThread(() -> {
                    if (response.profileCompleted) {
                        onLoginSuccess(response, false, null, null);
                    } else {
                        showGoogleProfileCompletionDialog(response);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            } catch (AuthApiClient.ApiException e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showGoogleProfileCompletionDialog(AuthApiClient.LoginResponse loginResponse) {
        EditText nameInput = createDialogInput("이름");
        EditText birthInput = createDialogInput("생년월일 (YYYY-MM-DD)");
        applyBirthDateAutoFormat(birthInput);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("추가 정보 입력")
                .setMessage("소셜 로그인 후 서비스 이용을 위해 이름과 생년월일을 입력해주세요.")
                .setView(createDialogLayout(nameInput, birthInput))
                .setCancelable(false)
                .setPositiveButton("완료", null)
                .create();
        dialog.show();

        android.widget.Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton == null) return;
        positiveButton.setOnClickListener(v -> {
            String name = getTrimmedText(nameInput);
            String birthDate = getTrimmedText(birthInput);

            if (name.isEmpty()) {
                nameInput.setError("이름을 입력해주세요.");
                nameInput.requestFocus();
                return;
            }
            if (!isValidBirthDateFormat(birthDate)) {
                birthInput.setError("생년월일 형식은 YYYY-MM-DD 입니다.");
                birthInput.requestFocus();
                return;
            }

            new Thread(() -> {
                try {
                    AuthApiClient.SocialProfileCompleteRequest request = new AuthApiClient.SocialProfileCompleteRequest();
                    request.userId = loginResponse.id;
                    request.name = name;
                    request.birthDate = birthDate;

                    AuthApiClient.LoginResponse completed = authApiClient.completeSocialProfile(request);
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(this, "추가 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        onLoginSuccess(completed, false, null, null);
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
                } catch (AuthApiClient.ApiException e) {
                    runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }

    private void onLoginSuccess(AuthApiClient.LoginResponse response,
                                boolean autoLoginChecked,
                                String loginId,
                                String password) {
        SettingsManager.saveLoginSession(this, response.id, response.loginId, response.name);

        if (response.birthDate != null) {
            SettingsManager.setBirthDate(this, response.birthDate);
        }
        if (response.email != null) {
            SettingsManager.setEmail(this, response.email);
        }

        if (autoLoginChecked && loginId != null && password != null) {
            SettingsManager.saveAutoLoginCredentials(this, loginId, password);
        } else {
            SettingsManager.clearAutoLoginCredentials(this);
        }

        Toast.makeText(this, response.name + "님, 환영합니다.", Toast.LENGTH_SHORT).show();
        MedicationServerSync.syncFromServer(this, () ->
                runOnUiThread(() -> {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
        );
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

    @SuppressLint("ClickableViewAccessibility")
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
}
