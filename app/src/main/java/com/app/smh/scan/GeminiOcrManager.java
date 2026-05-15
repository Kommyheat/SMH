package com.app.smh.scan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.app.smh.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;

public class GeminiOcrManager {

    public interface OcrCallback {
        void onSuccess(OcrResult result);
        void onError(String message);
    }

    private static final String TAG = "GeminiOCR";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private final GenerativeModelFutures model;

    public GeminiOcrManager() {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.responseMimeType = "application/json";
        GenerationConfig config = configBuilder.build();

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-pro",
                API_KEY,
                config
        );

        model = GenerativeModelFutures.from(gm);
    }

    public void analyzeImage(
            @NonNull Context context,
            @NonNull String source,
            @NonNull Uri imageUri,
            @Nullable File imageFile,
            @NonNull OcrCallback callback
    ) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onError("이미지를 읽을 수 없습니다.");
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                callback.onError("비트맵 변환 실패");
                return;
            }

            String prompt =
                    "이 이미지는 약봉투 또는 처방 관련 이미지다.\n" +
                            "이미지에 보이는 텍스트를 읽고, 약품명 후보만 추출하라.\n" +
                            "반드시 JSON으로만 응답하라.\n" +
                            "{\n" +
                            "  \"rawText\": \"이미지 전체 텍스트\",\n" +
                            "  \"drugCandidates\": [\"약품명후보1\", \"약품명후보2\"]\n" +
                            "}\n" +
                            "규칙:\n" +
                            "1. drugCandidates에는 약품명처럼 보이는 항목만 넣을 것.\n" +
                            "2. 병원명, 조제문구, 복약지도, 전화번호, 날짜는 제외할 것.\n" +
                            "3. JSON 외 다른 설명은 절대 쓰지 말 것.";

            Content content = new Content.Builder()
                    .addText(prompt)
                    .addImage(bitmap)
                    .build();

            Executor mainExecutor = ContextCompat.getMainExecutor(context);

            Futures.addCallback(
                    model.generateContent(content),
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            try {
                                String jsonResult = result.getText();
                                Log.d(TAG, "jsonResult = " + jsonResult);

                                if (jsonResult == null || jsonResult.trim().isEmpty()) {
                                    callback.onError("Gemini 응답 텍스트가 비어 있습니다.");
                                    return;
                                }

                                OcrResult ocrResult = parseOcrResult(jsonResult);
                                if (ocrResult == null) {
                                    callback.onError("OCR 결과 JSON 파싱 실패");
                                    return;
                                }

                                Log.d(TAG, "parsed drugNames = " + ocrResult.getDrugNames());
                                callback.onSuccess(ocrResult);

                            } catch (Exception e) {
                                Log.e(TAG, "OCR result handling failed", e);
                                callback.onError("OCR 결과 처리 실패: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.e(TAG, "Gemini API Error", t);
                            callback.onError("Gemini API 오류: " + t.getMessage());
                        }
                    },
                    mainExecutor
            );

        } catch (Exception e) {
            Log.e(TAG, "analyzeImage preparation failed", e);
            callback.onError("이미지 분석 준비 실패: " + e.getMessage());
        }
    }

    private OcrResult parseOcrResult(String jsonString) {
        try {
            String cleaned = jsonString.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JSONObject obj = new JSONObject(cleaned);

            String rawText = obj.optString("rawText", "");
            JSONArray drugCandidatesJson = obj.optJSONArray("drugCandidates");
            JSONArray drugNamesJson = obj.optJSONArray("drugNames");

            Set<String> uniqueNames = new LinkedHashSet<>();

            if (drugCandidatesJson != null) {
                extractDrugNamesFromArray(drugCandidatesJson, uniqueNames);
            }

            if (drugNamesJson != null) {
                extractDrugNamesFromArray(drugNamesJson, uniqueNames);
            }

            ArrayList<String> drugNames = new ArrayList<>(uniqueNames);
            return new OcrResult(rawText, drugNames);

        } catch (Exception e) {
            Log.e(TAG, "parseOcrResult failed", e);
            return null;
        }
    }

    private void extractDrugNamesFromArray(JSONArray jsonArray, Set<String> output) {
        for (int i = 0; i < jsonArray.length(); i++) {
            String value = jsonArray.optString(i, "");
            String refined = refineDrugCandidate(value);

            if (isMeaningfulDrugCandidate(refined)) {
                output.add(refined);
            }
        }
    }

    private String refineDrugCandidate(String input) {
        if (input == null) return "";

        String text = input.trim();

        text = text.replace("\\n", " ");
        text = text.replace("\\r", " ");

        // 괄호/대괄호/중괄호 및 내부 내용 제거
        text = text.replaceAll("\\([^\\)]*\\)", " ");
        text = text.replaceAll("\\[[^\\]]*\\]", " ");
        text = text.replaceAll("\\{[^\\}]*\\}", " ");

        // 언더바 및 특수문자 제거
        text = text.replaceAll("[_]+", " ");
        text = text.replaceAll("[^0-9a-zA-Z가-힣\\s]", " ");

        // 공백 정리
        text = text.replaceAll("\\s+", " ").trim();

        // 단위 중복/오타 보정
        text = text.replace("밀리그람", "밀리그램");
        text = text.replace("밀리그", "밀리그램");
        text = text.replace("밀리그램램", "밀리그램");
        text = text.replace("그램램", "그램");
        text = text.replace("그람", "그램");

        // 앞쪽 불필요 단어 제거
        text = text.replaceAll("^(비|씨|정제|캡슐제)\\s+", "");

        // 뒤에 붙는 잡단어 제거
        text = removeTrailingNoise(text);

        // 공백 다시 정리
        text = text.replaceAll("\\s+", "").trim();

        return text;
    }

    private String removeTrailingNoise(String text) {
        String[] trailingNoiseWords = {
                "세파클러", "트리", "아세트", "염산염", "복용", "식후", "식전", "보험",
                "조제", "처방", "투약", "주의", "약국", "병원", "의원"
        };

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String word : trailingNoiseWords) {
                if (text.endsWith(word) && text.length() > word.length() + 1) {
                    text = text.substring(0, text.length() - word.length()).trim();
                    changed = true;
                }
            }
        }

        return text;
    }

    private boolean isMeaningfulDrugCandidate(String text) {
        if (text == null || text.isEmpty()) return false;
        if (text.length() < 2) return false;

        String lower = text.toLowerCase();

        if (lower.contains("복용")
                || lower.contains("식후")
                || lower.contains("식전")
                || lower.contains("하루")
                || lower.contains("아침")
                || lower.contains("점심")
                || lower.contains("저녁")
                || lower.contains("취침")
                || lower.contains("병원")
                || lower.contains("의원")
                || lower.contains("약국")
                || lower.contains("전화")
                || lower.contains("용법")
                || lower.contains("용량")
                || lower.contains("주의")
                || lower.contains("투약")
                || lower.contains("조제")
                || lower.contains("처방")
                || lower.contains("번호")
                || lower.contains("환자")
                || lower.contains("성명")
                || lower.contains("보험")) {
            return false;
        }

        return text.matches(".*(정|캡슐|연질캡슐|서방정|현탁액|시럽|주사|크림|겔|패취|밀리그램|그램|mg).*");
    }
}
