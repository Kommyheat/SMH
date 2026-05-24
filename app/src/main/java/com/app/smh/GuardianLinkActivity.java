package com.app.smh;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.smh.auth.AuthApiClient;
import com.app.smh.calendar.PatientCalendarActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class GuardianLinkActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvMyUserCode;
    private ImageButton btnCopyUserCode;

    private EditText etUserCode;
    private LinearLayout btnRequestLink;

    private TextView tvProtectedUserName;
    private TextView tvProtectedUserStatus;
    private TextView tvMyGuardianName;
    private TextView tvMyGuardianStatus;

    // 수락/거절 버튼 영역
    private LinearLayout layoutAcceptReject;
    private LinearLayout btnAcceptLink;
    private LinearLayout btnRejectLink;

    private AuthApiClient authApiClient;
    private String currentUserCode = "";
    private long currentUserId = -1L;

    // 현재 careLinkId (수락/거절 시 필요)
    private long currentCareLinkId = -1L;
    // 연동 해제
    private LinearLayout layoutDisconnect;
    private LinearLayout btnDisconnectLink;
    // 달력 보기
    private LinearLayout layoutViewPatientCalendar;
    private LinearLayout btnViewPatientCalendar;
    private long currentPatientId = -1L;
    private String currentPatientName = "";


    // 스위치 관련
    private androidx.appcompat.widget.SwitchCompat switchShareStatus;
    private androidx.appcompat.widget.SwitchCompat switchMissedAlert;
    private LinearLayout layoutShareStatus;
    private LinearLayout layoutMissedAlert;
    private View dividerSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_link);

        authApiClient = new AuthApiClient();
        currentUserId = SettingsManager.getLoginUserId(this);

        initViews();
        setupListeners();
        bindInitialState();
        fetchMyUserCode();
        fetchCareLinkStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyUserCode();
        fetchCareLinkStatus();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvMyUserCode = findViewById(R.id.tv_my_invite_code);
        btnCopyUserCode = findViewById(R.id.btn_copy_invite_code);

        etUserCode = findViewById(R.id.et_invite_code);
        btnRequestLink = findViewById(R.id.btn_request_link);

        tvProtectedUserName = findViewById(R.id.tv_protected_user_name);
        tvProtectedUserStatus = findViewById(R.id.tv_protected_user_status);
        tvMyGuardianName = findViewById(R.id.tv_my_guardian_name);
        tvMyGuardianStatus = findViewById(R.id.tv_my_guardian_status);

        // 수락/거절
        layoutAcceptReject = findViewById(R.id.layout_accept_reject);
        btnAcceptLink = findViewById(R.id.btn_accept_link);
        btnRejectLink = findViewById(R.id.btn_reject_link);
        // 연동 해제
        layoutDisconnect = findViewById(R.id.layout_disconnect);
        btnDisconnectLink = findViewById(R.id.btn_disconnect_link);
        // 달력보기
        layoutViewPatientCalendar = findViewById(R.id.layout_view_patient_calendar);
        btnViewPatientCalendar = findViewById(R.id.btn_view_patient_calendar);
        // 스위치
        switchShareStatus = findViewById(R.id.switch_share_status);
        switchMissedAlert = findViewById(R.id.switch_missed_alert);
        layoutShareStatus = findViewById(R.id.layout_share_status);
        layoutMissedAlert = findViewById(R.id.layout_missed_alert);
        dividerSwitch = findViewById(R.id.divider_switch);
        // 저장된 설정 복원
        switchShareStatus.setChecked(
                SettingsManager.isShareStatusEnabled(this));
        switchMissedAlert.setChecked(
                SettingsManager.isMissedAlertEnabled(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCopyUserCode.setOnClickListener(v -> copyUserCode());

        btnRequestLink.setOnClickListener(v -> {
            String inputCode = etUserCode.getText().toString().trim().toUpperCase();

            if (currentUserId <= 0) {
                Toast.makeText(this, "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(inputCode)) {
                Toast.makeText(this, "사용자 코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!TextUtils.isEmpty(currentUserCode) && inputCode.equals(currentUserCode)) {
                Toast.makeText(this, "내 사용자 코드는 입력할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            showConfirmDialog(inputCode);
        });

        // 수락 버튼
        btnAcceptLink.setOnClickListener(v -> {
            if (currentCareLinkId <= 0) {
                Toast.makeText(this, "연동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("연동 수락")
                    .setMessage("보호자 연동 요청을 수락하시겠습니까?")
                    .setPositiveButton("수락", (dialog, which) -> submitAccept())
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 거절 버튼
        btnRejectLink.setOnClickListener(v -> {
            if (currentCareLinkId <= 0) {
                Toast.makeText(this, "연동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("연동 거절")
                    .setMessage("보호자 연동 요청을 거절하시겠습니까?")
                    .setPositiveButton("거절", (dialog, which) -> submitReject())
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 연동 해체 버튼
        btnDisconnectLink.setOnClickListener(v -> {
            if (currentCareLinkId <= 0) {
                Toast.makeText(this, "연동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("연동 해제")
                    .setMessage("보호자 연동을 해제하시겠습니까?")
                    .setPositiveButton("해제", (dialog, which) -> submitDisconnect())
                    .setNegativeButton("취소", null)
                    .show();
        });

        btnViewPatientCalendar.setOnClickListener(v -> {
            // patientId 확인 로그
            android.util.Log.d("PatientCal", "patientId: " + currentPatientId);
            android.util.Log.d("PatientCal", "patientName: " + currentPatientName);

            Intent intent = new Intent(GuardianLinkActivity.this,
                    PatientCalendarActivity.class);
            intent.putExtra("patientId", currentPatientId);
            intent.putExtra("patientName", currentPatientName);
            startActivity(intent);
        });

        // 공유 상태 스위치 변경 시 로컬 설정 저장
        if (switchShareStatus != null) {
            switchShareStatus.setOnCheckedChangeListener((buttonView, isChecked) -> SettingsManager.setShareStatusEnabled(this, isChecked));
        }

        // 복약 누락 알림 스위치 변경 시 로컬 설정 저장
        if (switchMissedAlert != null) {
            switchMissedAlert.setOnCheckedChangeListener((buttonView, isChecked) -> SettingsManager.setMissedAlertEnabled(this, isChecked));
        }
    }


    // 스위치 관련 영역 표시/숨김 처리
    private void setSwitchSectionVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;

        if (layoutShareStatus != null) {
            layoutShareStatus.setVisibility(visibility);
        }
        if (layoutMissedAlert != null) {
            layoutMissedAlert.setVisibility(visibility);
        }
        if (dividerSwitch != null) {
            dividerSwitch.setVisibility(visibility);
        }
    }

    private void bindInitialState() {
        tvMyUserCode.setText("불러오는 중...");
        tvProtectedUserName.setText("보호 중인 사용자 없음");
        tvProtectedUserStatus.setText("아직 연동 요청이 없어요");
        tvMyGuardianName.setText("연동된 보호자 없음");
        tvMyGuardianStatus.setText("아직 연결되지 않았어요");
        if (layoutAcceptReject != null) {
            layoutAcceptReject.setVisibility(View.GONE);
        }
        if (layoutDisconnect != null) {
            layoutDisconnect.setVisibility(View.GONE);
        }
        // 초기 상태 : 스위치 영역 숨김
        setSwitchSectionVisible(false);
    }

    private void fetchMyUserCode() {
        if (currentUserId <= 0) {
            currentUserCode = "";
            tvMyUserCode.setText("-");
            return;
        }

        new Thread(() -> {
            try {
                AuthApiClient.LinkCodeResponse response = authApiClient.getMyLinkCode(currentUserId);
                runOnUiThread(() -> {
                    currentUserCode = response != null && response.linkCode != null
                            ? response.linkCode.trim() : "";
                    tvMyUserCode.setText(currentUserCode.isEmpty() ? "-" : currentUserCode);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    currentUserCode = "";
                    tvMyUserCode.setText("-");
                });
            }
        }).start();
    }

    private void fetchCareLinkStatus() {
        if (currentUserId <= 0) {
            renderEmptyState();
            return;
        }

        new Thread(() -> {
            try {
                AuthApiClient.CareLinkStatusResponse response =
                        authApiClient.getCareLinkStatus(currentUserId);
                runOnUiThread(() -> renderCareLinkStatus(response));
            } catch (Exception e) {
                runOnUiThread(this::renderEmptyState);
            }
        }).start();
    }

    private void showConfirmDialog(String inputCode) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("보호자 연동")
                .setMessage("입력한 사용자 코드로 보호자 연동 요청을 보내시겠습니까?")
                .setPositiveButton("요청 보내기", (dialog, which) -> submitCareLinkRequest(inputCode))
                .setNegativeButton("취소", null)
                .show();
    }

    private void submitCareLinkRequest(String inputCode) {
        new Thread(() -> {
            try {
                AuthApiClient.CareLinkRequest request = new AuthApiClient.CareLinkRequest();
                request.caregiverId = currentUserId;
                request.patientUserCode = inputCode;

                AuthApiClient.MessageResponse response = authApiClient.requestCareLink(request);

                runOnUiThread(() -> {
                    etUserCode.setText("");
                    Toast.makeText(this,
                            response != null && response.message != null
                                    ? response.message : "연동 요청을 보냈습니다.",
                            Toast.LENGTH_SHORT).show();
                    fetchCareLinkStatus();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage() != null ? e.getMessage() : "연동 요청에 실패했습니다.",
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // 수락 API 호출
    private void submitAccept() {
        new Thread(() -> {
            try {
                AuthApiClient.CareLinkDecisionRequest request =
                        new AuthApiClient.CareLinkDecisionRequest();
                request.patientId = currentUserId;
                request.careLinkId = currentCareLinkId;

                AuthApiClient.MessageResponse response = authApiClient.acceptCareLink(request);

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            response != null && response.message != null
                                    ? response.message : "연동 요청을 수락했습니다.",
                            Toast.LENGTH_SHORT).show();
                    fetchCareLinkStatus();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage() != null ? e.getMessage() : "수락에 실패했습니다.",
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


    // 거절 API 호출
    private void submitReject() {
        new Thread(() -> {
            try {
                AuthApiClient.CareLinkDecisionRequest request =
                        new AuthApiClient.CareLinkDecisionRequest();
                request.patientId = currentUserId;
                request.careLinkId = currentCareLinkId;

                AuthApiClient.MessageResponse response = authApiClient.rejectCareLink(request);

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            response != null && response.message != null
                                    ? response.message : "연동 요청을 거절했습니다.",
                            Toast.LENGTH_SHORT).show();
                    fetchCareLinkStatus();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage() != null ? e.getMessage() : "거절에 실패했습니다.",
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void submitDisconnect() {
        new Thread(() -> {
            try {
                // DELETE /api/care-links/{linkId}?caregiverId={caregiverId}
                // caregiverId는 caregiver 또는 patient 중 요청하는 사람의 userId
                authApiClient.disconnectCareLink(currentUserId, currentCareLinkId);

                runOnUiThread(() -> {
                    Toast.makeText(this, "연동이 해제되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchCareLinkStatus();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage() != null ? e.getMessage() : "연동 해제에 실패했습니다.",
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void renderCareLinkStatus(AuthApiClient.CareLinkStatusResponse response) {
        if (response == null || response.status == null || response.status.trim().isEmpty()) {
            renderEmptyState();
            return;
        }
        // careLinkId 저장
        currentCareLinkId = response.id;

        String status = response.status.trim().toUpperCase();
        String protectedName = isEmpty(response.patientName) ? "이름 없음" : response.patientName;
        String guardianName = isEmpty(response.caregiverName) ? "이름 없음" : response.caregiverName;

        // 수락/거절 버튼 숨기기
        layoutAcceptReject.setVisibility(View.GONE);

        // 모든 케이스 시작 전 피보호자 달력 버튼 숨기기
        if (layoutViewPatientCalendar != null) {
            layoutViewPatientCalendar.setVisibility(View.GONE);
        }
        setSwitchSectionVisible(false);

        switch (status) {
            case "PENDING":
                tvProtectedUserName.setText("보호 중인 사용자: " + protectedName);
                tvProtectedUserStatus.setText("상대방 수락 대기 중");

                if (response.caregiverId == currentUserId) {
                    // 내가 요청 보낸 쪽 → 버튼 없음
                    tvMyGuardianName.setText("연동된 보호자 없음");
                    tvMyGuardianStatus.setText("내가 보낸 요청을 기다리는 중이에요");
                } else {
                    // 내가 요청 받은 쪽 → 수락/거절 버튼 표시
                    tvMyGuardianName.setText("보호자 요청 도착: " + guardianName);
                    tvMyGuardianStatus.setText("수락 또는 거절을 선택해주세요");
                    layoutAcceptReject.setVisibility(View.VISIBLE);
                }
                break;

            case "ACTIVE":
                tvProtectedUserName.setText("보호 중인 사용자: " + protectedName);
                tvProtectedUserStatus.setText("현재 연동 완료");
                tvMyGuardianName.setText("연동된 보호자: " + guardianName);
                tvMyGuardianStatus.setText("복약 현황을 함께 확인할 수 있어요");
                // 연동 해제 버튼 표시
                layoutDisconnect.setVisibility(View.VISIBLE);
                // ACTIVE 상태에서는 스위치 영역 표시
                setSwitchSectionVisible(true);

                // layoutViewPatientCalendar 사용
                // 내가 보호자(caregiver)일 때만 피보호자 달력 버튼 표시
                if (response.caregiverId == currentUserId) {
                    currentPatientId = response.patientId;
                    currentPatientName = protectedName;
                    if (layoutViewPatientCalendar != null) {
                        layoutViewPatientCalendar.setVisibility(View.VISIBLE);
                    }
                }
                break;

            case "DISCONNECTED":
                tvProtectedUserName.setText("보호 중인 사용자 없음");
                tvProtectedUserStatus.setText("연동이 해제되었어요");
                tvMyGuardianName.setText("연동된 보호자 없음");
                tvMyGuardianStatus.setText("현재 연결된 보호자가 없어요");
                break;

            case "REJECTED":
                tvProtectedUserName.setText("보호 중인 사용자 없음");
                tvProtectedUserStatus.setText("연동 요청이 거절되었어요");
                tvMyGuardianName.setText("연동된 보호자 없음");
                tvMyGuardianStatus.setText("새 요청을 다시 보낼 수 있어요");
                break;

            default:
                renderEmptyState();
                break;
        }
    }

    private void renderEmptyState() {
        currentCareLinkId = -1L;
        currentPatientId = -1L;
        currentPatientName = "";
        tvProtectedUserName.setText("보호 중인 사용자 없음");
        tvProtectedUserStatus.setText("아직 연동 요청이 없어요");
        tvMyGuardianName.setText("연동된 보호자 없음");
        tvMyGuardianStatus.setText("아직 연결되지 않았어요");
        if (layoutAcceptReject != null) {
            layoutAcceptReject.setVisibility(View.GONE);
        }
        if (layoutDisconnect != null) {
            layoutDisconnect.setVisibility(View.GONE);
        }
        // 피보호자 달력 버튼 숨기기
        if (layoutViewPatientCalendar != null) {
            layoutViewPatientCalendar.setVisibility(View.GONE);
        }
        // 연동 정보가 없으면 스위치 영역 숨김
        setSwitchSectionVisible(false);

    }
    private void copyUserCode() {
        if (currentUserCode == null || currentUserCode.trim().isEmpty()) {
            Toast.makeText(this, "복사할 사용자 코드가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("사용자 코드", currentUserCode));
            Toast.makeText(this, "사용자 코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
