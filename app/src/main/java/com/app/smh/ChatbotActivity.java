package com.app.smh;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ChatbotActivity extends AppCompatActivity {

    private ImageButton btnBackChatbot;
    private ImageButton btnSendChat;
    private EditText etChatInput;
    private TextView tvUserQuestion;
    private TextView tvChatbotResponse;

    private Button btnQTime, btnQSideEffect, btnQInteraction, btnQScanInfo, btnQGuardian;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        btnBackChatbot = findViewById(R.id.btn_back_chatbot);
        btnSendChat = findViewById(R.id.btn_send_chat);
        etChatInput = findViewById(R.id.et_chat_input);
        tvUserQuestion = findViewById(R.id.tv_user_question);
        tvChatbotResponse = findViewById(R.id.tv_chatbot_response);

        btnQTime = findViewById(R.id.btn_q_time);
        btnQSideEffect = findViewById(R.id.btn_q_side_effect);
        btnQInteraction = findViewById(R.id.btn_q_interaction);
        btnQScanInfo = findViewById(R.id.btn_q_scan_info);
        btnQGuardian = findViewById(R.id.btn_q_guardian);

        bottomNavigationView = findViewById(R.id.bottom_nav_chat);

        btnBackChatbot.setOnClickListener(v -> finish());

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> true);

        btnQTime.setOnClickListener(v -> setQuestionAndAnswer(
                "복용 시간 방법",
                "복용 시간 방법은 식전, 식후, 취침 전 여부를 기준으로 안내할 예정입니다."
        ));

        btnQSideEffect.setOnClickListener(v -> setQuestionAndAnswer(
                "부작용",
                "부작용 정보는 약품별 안내 데이터와 주의사항 정보를 바탕으로 제공할 예정입니다."
        ));

        btnQInteraction.setOnClickListener(v -> setQuestionAndAnswer(
                "병용 금지",
                "병용 금지 정보는 함께 등록된 약 정보를 비교해 주의해야 할 조합을 보여줄 예정입니다."
        ));

        btnQScanInfo.setOnClickListener(v -> setQuestionAndAnswer(
                "스캔한 약 상세 설명",
                "스캔한 약 상세 설명은 OCR 결과와 약품 상세 조회 데이터를 연결해 보여줄 예정입니다."
        ));

        btnQGuardian.setOnClickListener(v -> setQuestionAndAnswer(
                "보호자용 상태 요약 보기",
                "보호자용 상태 요약은 복약 완료 여부와 최근 상태를 정리해 보여줄 예정입니다."
        ));

        btnSendChat.setOnClickListener(v -> {
            String input = etChatInput.getText().toString().trim();

            if (!input.isEmpty()) {
                tvUserQuestion.setText(input);
                tvChatbotResponse.setText("입력한 질문에 대한 AI 상담 기능은 이후 연결될 예정입니다.");
                etChatInput.setText("");
            }

            hideKeyboard();
            etChatInput.clearFocus();
        });
    }

    private void setQuestionAndAnswer(String question, String answer) {
        tvUserQuestion.setText(question);
        tvChatbotResponse.setText(answer);
        hideKeyboard();
        etChatInput.clearFocus();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}