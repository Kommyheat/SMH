package com.app.smh;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class InquiryActivity extends AppCompatActivity {

    private EditText etInquiryTitle;
    private EditText etInquiryContent;
    private EditText etEmailLocal;
    private EditText etEmailDomain;
    private TextView tvFileName;
    private LinearLayout layoutFileAttach;
    private LinearLayout btnSubmitInquiry;

    private Uri selectedFileUri = null;

    // 파일 선택
    private final ActivityResultLauncher<String> pickFile =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    String fileName = getFileName(uri);
                    tvFileName.setText(fileName != null ? fileName : "파일 선택됨");
                    tvFileName.setTextColor(getColor(R.color.black));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiry);

        initViews();
        setupListeners();
        prefillEmail();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        etInquiryTitle = findViewById(R.id.et_inquiry_title);
        etInquiryContent = findViewById(R.id.et_inquiry_content);
        etEmailLocal = findViewById(R.id.et_email_local);
        etEmailDomain = findViewById(R.id.et_email_domain);
        tvFileName = findViewById(R.id.tv_file_name);
        layoutFileAttach = findViewById(R.id.layout_file_attach);
        btnSubmitInquiry = findViewById(R.id.btn_submit_inquiry);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // 파일 첨부 클릭
        layoutFileAttach.setOnClickListener(v ->
                pickFile.launch("*/*")
        );

        // 문의하기 버튼
        btnSubmitInquiry.setOnClickListener(v -> submitInquiry());
    }

    // 로그인된 이메일 자동 입력
    private void prefillEmail() {
        String savedEmail = SettingsManager.getEmail(this);
        if (savedEmail != null && !savedEmail.isEmpty() && savedEmail.contains("@")) {
            String[] parts = savedEmail.split("@");
            if (parts.length == 2) {
                etEmailLocal.setText(parts[0]);
                etEmailDomain.setText(parts[1]);
            }
        }
    }

    private void submitInquiry() {
        String title = etInquiryTitle.getText().toString().trim();
        String content = etInquiryContent.getText().toString().trim();
        String emailLocal = etEmailLocal.getText().toString().trim();
        String emailDomain = etEmailDomain.getText().toString().trim();

        // 유효성 검사
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "문의 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etInquiryTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "문의 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etInquiryContent.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(emailLocal) || TextUtils.isEmpty(emailDomain)) {
            Toast.makeText(this, "답변 받을 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            etEmailLocal.requestFocus();
            return;
        }

        String fullEmail = emailLocal + "@" + emailDomain;

        // TODO: 백엔드 문의 API 연동 시 여기에 추가
        // 현재는 완료 토스트만 표시
        Toast.makeText(this, "문의가 접수되었습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    // 파일명 추출
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
