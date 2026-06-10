package com.app.smh;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.app.smh.api.ChatbotApiClient;

public class ChatbotActivity extends AppCompatActivity {

    private ImageButton btnBackChatbot;
    private ImageButton btnSendChat;
    private EditText etChatInput;

    private Button btnQTime, btnQSideEffect, btnQInteraction, btnQScanInfo, btnQGuardian;
    private BottomNavigationView bottomNavigationView;

    private final ChatbotApiClient chatbotApiClient = new ChatbotApiClient();

    private LinearLayout chatContainer;
    private ScrollView chatScroll;

    private boolean isWaitingForAnswer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        btnBackChatbot = findViewById(R.id.btn_back_chatbot);
        btnSendChat = findViewById(R.id.btn_send_chat);
        etChatInput = findViewById(R.id.et_chat_input);
        chatContainer = findViewById(R.id.chat_container);
        chatScroll = findViewById(R.id.chat_scroll);

        btnQTime = findViewById(R.id.btn_q_time);
        btnQSideEffect = findViewById(R.id.btn_q_side_effect);
        btnQInteraction = findViewById(R.id.btn_q_interaction);
        btnQScanInfo = findViewById(R.id.btn_q_scan_info);
        btnQGuardian = findViewById(R.id.btn_q_guardian);

        bottomNavigationView = findViewById(R.id.bottom_nav_chat);

        btnBackChatbot.setOnClickListener(v -> finish());


        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> true);

        btnQTime.setOnClickListener(v -> askChatbot("복용 시간 방법"));
        btnQSideEffect.setOnClickListener(v -> askChatbot("부작용"));
        btnQInteraction.setOnClickListener(v -> askChatbot("병용 금지"));
        btnQScanInfo.setOnClickListener(v -> askChatbot("스캔한 약 상세 설명"));
        btnQGuardian.setOnClickListener(v -> askChatbot("보호자용 상태 요약 보기"));

        btnSendChat.setOnClickListener(v -> {

            if (isWaitingForAnswer) return;

            String input = etChatInput.getText().toString().trim();

            if (!input.isEmpty()) {
                etChatInput.setText("");
                askChatbot(input);
            }

            hideKeyboard();
            etChatInput.clearFocus();
        });
    }

    private void setQuestionAndAnswer(String question, String answer) {
        askChatbot(question);
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

    private void askChatbot(String question) {
        if (isWaitingForAnswer) return;

        isWaitingForAnswer = true;
        btnSendChat.setEnabled(false);

        addUserMessage(question);

        TextView loadingMessage = addBotMessage("답변을 준비하고 있어요...");

        new Thread(() -> {
            try {
                ChatbotApiClient.ChatbotResponse response =
                        chatbotApiClient.sendMessage(
                                new ChatbotApiClient.ChatbotRequest(question)
                        );

                runOnUiThread(() -> {
                    if (response != null
                            && response.answer != null
                            && !response.answer.trim().isEmpty()) {

                        loadingMessage.setText(response.answer);

                    } else {
                        loadingMessage.setText("응답을 가져오지 못했습니다.");
                    }

                    isWaitingForAnswer = false;
                    btnSendChat.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingMessage.setText("서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.");

                    isWaitingForAnswer = false;
                    btnSendChat.setEnabled(true);
                });
            }
        }).start();

        hideKeyboard();
        etChatInput.clearFocus();
    }

    private void addUserMessage(String message) {
        TextView tv = new TextView(this);

        tv.setText(message);
        tv.setPadding(30, 20, 30, 20);
        tv.setTextColor(ContextCompat.getColor(this, R.color.black));
        tv.setTextSize(15);
        tv.setBackgroundResource(R.drawable.bg_quick_question_selected);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(100, 20, 0, 0);
        params.gravity = android.view.Gravity.END;

        tv.setLayoutParams(params);
        chatContainer.addView(tv);

        scrollToBottom();
    }

    private TextView addBotMessage(String message) {
        TextView tv = new TextView(this);

        tv.setText(message);
        tv.setPadding(30, 20, 30, 20);
        tv.setTextColor(ContextCompat.getColor(this, R.color.black));
        tv.setTextSize(15);
        tv.setBackgroundResource(R.drawable.bg_chat_bubble_left);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 20, 100, 0);

        tv.setLayoutParams(params);
        chatContainer.addView(tv);

        scrollToBottom();

        return tv;
    }

    private void scrollToBottom() {
        if (chatScroll != null) {
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
        }
    }

}
