package com.app.smh.scan;

import android.util.Log;

import androidx.annotation.NonNull;

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

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class GeminiDrugGroupManager {

    public interface GroupCallback {
        void onSuccess(ArrayList<ScanAnalysisGroupItem> groups);
        void onError(String message);
    }

    private static final String TAG = "GeminiDrugGroup";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private final GenerativeModelFutures model;

    public GeminiDrugGroupManager() {
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

    public void analyzeDrugGroups(@NonNull ArrayList<String> drugNames,
                                  @NonNull String rawText,
                                  @NonNull GroupCallback callback) {

        try {
            StringBuilder drugListBuilder = new StringBuilder();
            for (String name : drugNames) {
                drugListBuilder.append("- ").append(name).append("\n");
            }

            String prompt =
                    "다음은 약봉투 OCR 결과와 추출된 약품명 목록이다.\n" +
                            "약품명 개수와 약 성격을 분석해 관련 약끼리 그룹으로 묶어라.\n" +
                            "복용기간과 복용시간(아침/점심/저녁)을 추론하라.\n" +
                            "같은 시간에 복용하는 약은 하나의 그룹으로 묶어라.\n" +
                            "복용시간이 여러 개인 경우 intakeTimes 배열에 모두 포함하라.\n" +
                            "취침 전 계열 약이면 bedtimeGroup=true로 표시하고 intakeTimes에 저녁을 포함하라.\n" +
                            "확실하지 않은 날짜는 빈 문자열로 두어라.\n" +
                            "JSON으로만 응답하라.\n\n" +
                            "[OCR rawText]\n" + rawText + "\n\n" +
                            "[drugNames]\n" + drugListBuilder + "\n" +
                            "출력 형식:\n" +
                            "{\n" +
                            "  \"groups\": [\n" +
                            "    {\n" +
                            "      \"groupTitle\": \"감기약\",\n" +
                            "      \"drugNames\": [\"코미정\", \"슈다페드정\"],\n" +
                            "      \"startDate\": \"2026-05-01\",\n" +
                            "      \"endDate\": \"2026-05-05\",\n" +
                            "      \"intakeTimes\": [\"아침\", \"점심\", \"저녁\"],\n" +
                            "      \"bedtimeGroup\": false\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}";

            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();

            Futures.addCallback(
                    model.generateContent(content),
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            try {
                                String json = result.getText();
                                Log.d(TAG, "group json = " + json);

                                ArrayList<ScanAnalysisGroupItem> groups = parseGroups(json);
                                callback.onSuccess(groups);

                            } catch (Exception e) {
                                Log.e(TAG, "parse group result failed", e);
                                callback.onError("약 그룹 분석 결과 처리 실패");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            Log.e(TAG, "Gemini group analyze error", t);
                            callback.onError("약 그룹 분석 실패: " + t.getMessage());
                        }
                    },
                    Executors.newSingleThreadExecutor()
            );

        } catch (Exception e) {
            Log.e(TAG, "analyzeDrugGroups exception", e);
            callback.onError("약 그룹 분석 준비 실패");
        }
    }

    private ArrayList<ScanAnalysisGroupItem> parseGroups(String jsonString) throws Exception {
        ArrayList<ScanAnalysisGroupItem> result = new ArrayList<>();

        String cleaned = jsonString.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        JSONObject root = new JSONObject(cleaned);
        JSONArray groups = root.optJSONArray("groups");

        if (groups == null) return result;

        for (int i = 0; i < groups.length(); i++) {
            JSONObject obj = groups.optJSONObject(i);
            if (obj == null) continue;

            ScanAnalysisGroupItem item = new ScanAnalysisGroupItem();
            item.setGroupTitle(obj.optString("groupTitle", ""));

            JSONArray names = obj.optJSONArray("drugNames");
            ArrayList<String> drugs = new ArrayList<>();
            if (names != null) {
                for (int j = 0; j < names.length(); j++) {
                    String name = names.optString(j, "").trim();
                    if (!name.isEmpty()) {
                        drugs.add(name);
                    }
                }
            }

            item.setDrugNames(drugs);
            item.setStartDate(obj.optString("startDate", ""));
            item.setEndDate(obj.optString("endDate", ""));
            item.setIntakeTime(obj.optString("intakeTime", ""));
            item.setBedtimeGroup(obj.optBoolean("bedtimeGroup", false));

            // 수정: intakeTimes 배열 파싱
            ArrayList<String> intakeTimes = new ArrayList<>();
            JSONArray timesArray = obj.optJSONArray("intakeTimes");
            if (timesArray != null) {
                for (int j = 0; j < timesArray.length(); j++) {
                    String t = timesArray.optString(j, "").trim();
                    if (!t.isEmpty()) intakeTimes.add(t);
                }
            } else {
                // 하위 호환: intakeTime 단일값도 처리
                String single = obj.optString("intakeTime", "");
                if (!single.isEmpty()) intakeTimes.add(single);
            }
            item.setIntakeTimes(intakeTimes);

            result.add(item);
        }

        return result;
    }
}
