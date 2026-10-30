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

import java.util.List;

public class GuardianLinkActivity extends BaseActivity {

    private ImageButton btnBack;
    private TextView tvMyUserCode;
    private ImageButton btnCopyUserCode;
    private EditText etUserCode;
    private LinearLayout btnRequestLink;

    // 수락/거절 (PENDING + 내가 patient)
    private LinearLayout layoutAcceptReject;
    private LinearLayout btnAcceptLink;
    private LinearLayout btnRejectLink;

    // 연동된 사람 목록
    private LinearLayout layoutConnectedList;
    private TextView tvConnectedLabel;
    private TextView tvEmptyState;

    // 수락 대기 목록
    private LinearLayout layoutPendingList;
    private TextView tvPendingLabel;

    // 스위치
    private androidx.appcompat.widget.SwitchCompat switchShareStatus;
    private androidx.appcompat.widget.SwitchCompat switchMissedAlert;
    private LinearLayout layoutShareStatus;
    private LinearLayout layoutMissedAlert;
    private View dividerSwitch;

    private AuthApiClient authApiClient;
    private String currentUserCode = "";
    private long currentUserId = -1L;
    private long currentCareLinkId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_link);

        authApiClient = new AuthApiClient();
        currentUserId = SettingsManager.getLoginUserId(this);

        initViews();
        setupListeners();
        fetchMyUserCode();
        fetchCareLinkList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyUserCode();
        fetchCareLinkList();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvMyUserCode = findViewById(R.id.tv_my_invite_code);
        btnCopyUserCode = findViewById(R.id.btn_copy_invite_code);
        etUserCode = findViewById(R.id.et_invite_code);
        btnRequestLink = findViewById(R.id.btn_request_link);

        layoutAcceptReject = findViewById(R.id.layout_accept_reject);
        btnAcceptLink = findViewById(R.id.btn_accept_link);
        btnRejectLink = findViewById(R.id.btn_reject_link);

        layoutConnectedList = findViewById(R.id.layout_connected_list);
        tvConnectedLabel = findViewById(R.id.tv_connected_label);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        layoutPendingList = findViewById(R.id.layout_pending_list);
        tvPendingLabel = findViewById(R.id.tv_pending_label);

        switchShareStatus = findViewById(R.id.switch_share_status);
        switchMissedAlert = findViewById(R.id.switch_missed_alert);
        layoutShareStatus = findViewById(R.id.layout_share_status);
        layoutMissedAlert = findViewById(R.id.layout_missed_alert);
        dividerSwitch = findViewById(R.id.divider_switch);

        if (switchShareStatus != null)
            switchShareStatus.setChecked(SettingsManager.isShareStatusEnabled(this));
        if (switchMissedAlert != null)
            switchMissedAlert.setChecked(SettingsManager.isMissedAlertEnabled(this));
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

        btnAcceptLink.setOnClickListener(v -> {
            if (currentCareLinkId <= 0) {
                Toast.makeText(this, "연동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("연동 수락")
                    .setMessage("연동 요청을 수락하시겠습니까?")
                    .setPositiveButton("수락", (d, w) -> submitAccept())
                    .setNegativeButton("취소", null)
                    .show();
        });

        btnRejectLink.setOnClickListener(v -> {
            if (currentCareLinkId <= 0) {
                Toast.makeText(this, "연동 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("연동 거절")
                    .setMessage("연동 요청을 거절하시겠습니까?")
                    .setPositiveButton("거절", (d, w) -> submitReject())
                    .setNegativeButton("취소", null)
                    .show();
        });

        if (switchShareStatus != null)
            switchShareStatus.setOnCheckedChangeListener((btn, isChecked) ->
                    SettingsManager.setShareStatusEnabled(this, isChecked));
        if (switchMissedAlert != null)
            switchMissedAlert.setOnCheckedChangeListener((btn, isChecked) ->
                    SettingsManager.setMissedAlertEnabled(this, isChecked));
    }

    private void fetchMyUserCode() {
        if (currentUserId <= 0) { tvMyUserCode.setText("-"); return; }
        new Thread(() -> {
            try {
                AuthApiClient.LinkCodeResponse response =
                        authApiClient.getMyLinkCode(currentUserId);
                runOnUiThread(() -> {
                    currentUserCode = response != null && response.linkCode != null
                            ? response.linkCode.trim() : "";
                    tvMyUserCode.setText(currentUserCode.isEmpty() ? "-" : currentUserCode);
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvMyUserCode.setText("-"));
            }
        }).start();
    }

    /**
     * 전체 연동 목록 조회 (다중 연동 지원)
     */
    private void fetchCareLinkList() {
        if (currentUserId <= 0) return;
        new Thread(() -> {
            try {
                // 1. ACTIVE 목록
                List<AuthApiClient.CareLinkStatusResponse> activeList =
                        authApiClient.getCareLinkList(currentUserId);
                // 2. 단일 상태 (PENDING 체크용)
                AuthApiClient.CareLinkStatusResponse singleStatus =
                        authApiClient.getCareLinkStatus(currentUserId);

                runOnUiThread(() -> renderCareLinkList(activeList, singleStatus));
            } catch (Exception e) {
                runOnUiThread(this::renderEmptyState);
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void renderCareLinkList(
            List<AuthApiClient.CareLinkStatusResponse> activeList,
            AuthApiClient.CareLinkStatusResponse singleStatus) {

        // 초기화
        layoutConnectedList.removeAllViews();
        layoutPendingList.removeAllViews();
        layoutAcceptReject.setVisibility(View.GONE);
        tvPendingLabel.setVisibility(View.GONE);
        layoutPendingList.setVisibility(View.GONE);
        tvConnectedLabel.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        setSwitchSectionVisible(false);

        // PENDING 처리 (수락/거절 버튼)
        if (singleStatus != null && "PENDING".equals(singleStatus.status)) {
            currentCareLinkId = singleStatus.id;
            String guardianName = isEmpty(singleStatus.caregiverName)
                    ? "알 수 없음" : singleStatus.caregiverName;

            if (singleStatus.caregiverId != currentUserId) {
                // 내가 받은 요청 → 수락/거절 버튼 표시
                tvPendingLabel.setVisibility(View.VISIBLE);
                layoutPendingList.setVisibility(View.VISIBLE);

                TextView tvPending = new TextView(this);
                tvPending.setText("📨 " + guardianName + "님이 연동을 요청했어요");
                tvPending.setTextColor(0xFF111111);
                tvPending.setTextSize(14f);
                tvPending.setPadding(0, 8, 0, 8);
                layoutPendingList.addView(tvPending);

                layoutAcceptReject.setVisibility(View.VISIBLE);
            } else {
                // 내가 보낸 요청 → 대기 중 표시
                tvPendingLabel.setVisibility(View.VISIBLE);
                layoutPendingList.setVisibility(View.VISIBLE);

                TextView tvPending = new TextView(this);
                String patientName = isEmpty(singleStatus.patientName)
                        ? "알 수 없음" : singleStatus.patientName;
                tvPending.setText("⏳ " + patientName + "님의 수락을 기다리고 있어요");
                tvPending.setTextColor(0xFF888888);
                tvPending.setTextSize(14f);
                tvPending.setPadding(0, 8, 0, 8);
                layoutPendingList.addView(tvPending);
            }
        }

        // ACTIVE 목록 처리
        if (activeList == null || activeList.isEmpty()) {
            if (singleStatus == null || !"PENDING".equals(singleStatus.status)) {
                tvEmptyState.setVisibility(View.VISIBLE);
            }
            return;
        }

        tvConnectedLabel.setVisibility(View.VISIBLE);
        setSwitchSectionVisible(true);

        for (AuthApiClient.CareLinkStatusResponse link : activeList) {
            // 상대방 이름과 역할 결정
            boolean iAmCaregiver = link.caregiverId == currentUserId;
            String partnerName = iAmCaregiver
                    ? (isEmpty(link.patientName) ? "알 수 없음" : link.patientName)
                    : (isEmpty(link.caregiverName) ? "알 수 없음" : link.caregiverName);
            String roleLabel = iAmCaregiver ? "피보호자" : "보호자";
            long partnerId = iAmCaregiver ? link.patientId : link.caregiverId;

            // 버튼 형태로 추가
            LinearLayout btnItem = new LinearLayout(this);
            btnItem.setOrientation(LinearLayout.HORIZONTAL);
            btnItem.setBackgroundResource(R.drawable.bg_alarm_item_new);
            btnItem.setClickable(true);
            btnItem.setFocusable(true);
            btnItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
            btnItem.setPadding(40, 30, 40, 30);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            btnItem.setLayoutParams(params);

            // 이름 + 역할
            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textLayout.setLayoutParams(textParams);

            TextView tvName = new TextView(this);
            tvName.setText(partnerName);
            tvName.setTextColor(0xFF111111);
            tvName.setTextSize(15f);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvRole = new TextView(this);
            tvRole.setText(roleLabel);
            tvRole.setTextColor(0xFF888888);
            tvRole.setTextSize(12f);
            tvRole.setPadding(0, 4, 0, 0);

            textLayout.addView(tvName);
            textLayout.addView(tvRole);
            btnItem.addView(textLayout);

            // 달력 보기 아이콘
            TextView tvArrow = new TextView(this);
            tvArrow.setText(">");
            tvArrow.setTextColor(0xFFCCCCCC);
            tvArrow.setTextSize(16f);
            btnItem.addView(tvArrow);

            // 클릭 시 해당 사람의 달력으로 이동
            final long finalPartnerId = partnerId;
            final String finalPartnerName = partnerName;
            final long finalLinkId = link.id;

            btnItem.setOnClickListener(v -> {
                Intent intent = new Intent(GuardianLinkActivity.this,
                        PatientCalendarActivity.class);
                intent.putExtra("patientId", finalPartnerId);
                intent.putExtra("patientName", finalPartnerName);
                startActivity(intent);
            });

            // 길게 누르면 연동 해제
            btnItem.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("연동 해제")
                        .setMessage(finalPartnerName + "님과의 연동을 해제하시겠습니까?")
                        .setPositiveButton("해제", (d, w) -> {
                            new Thread(() -> {
                                try {
                                    authApiClient.disconnectCareLink(currentUserId, finalLinkId);
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "연동이 해제되었습니다.",
                                                Toast.LENGTH_SHORT).show();
                                        fetchCareLinkList();
                                    });
                                } catch (Exception e) {
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "해제에 실패했습니다.",
                                                    Toast.LENGTH_SHORT).show());
                                }
                            }).start();
                        })
                        .setNegativeButton("취소", null)
                        .show();
                return true;
            });

            layoutConnectedList.addView(btnItem);
        }
    }

    private void renderEmptyState() {
        layoutConnectedList.removeAllViews();
        layoutPendingList.removeAllViews();
        layoutAcceptReject.setVisibility(View.GONE);
        tvPendingLabel.setVisibility(View.GONE);
        layoutPendingList.setVisibility(View.GONE);
        tvConnectedLabel.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        setSwitchSectionVisible(false);
        currentCareLinkId = -1L;
    }

    private void setSwitchSectionVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        if (layoutShareStatus != null) layoutShareStatus.setVisibility(v);
        if (layoutMissedAlert != null) layoutMissedAlert.setVisibility(v);
        if (dividerSwitch != null) dividerSwitch.setVisibility(v);
    }

    private void showConfirmDialog(String inputCode) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("연동 요청")
                .setMessage("입력한 코드로 연동 요청을 보내시겠습니까?")
                .setPositiveButton("요청", (d, w) -> submitCareLinkRequest(inputCode))
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
                    fetchCareLinkList();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                e.getMessage() != null ? e.getMessage() : "요청에 실패했습니다.",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

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
                                    ? response.message : "수락했습니다.",
                            Toast.LENGTH_SHORT).show();
                    fetchCareLinkList();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "수락에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

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
                                    ? response.message : "거절했습니다.",
                            Toast.LENGTH_SHORT).show();
                    fetchCareLinkList();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "거절에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void copyUserCode() {
        if (currentUserCode == null || currentUserCode.trim().isEmpty()) {
            Toast.makeText(this, "복사할 사용자 코드가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("사용자 코드", currentUserCode));
            Toast.makeText(this, "사용자 코드를 복사했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
