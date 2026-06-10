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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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

    // 오늘 날짜 반환
    private String getTodayString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void analyzeDrugGroups(@NonNull ArrayList<String> drugNames,
                                  @NonNull String rawText,
                                  @NonNull GroupCallback callback) {

        try {
            StringBuilder drugListBuilder = new StringBuilder();
            for (String name : drugNames) {
                drugListBuilder.append("- ").append(name).append("\n");
            }

            String today = getTodayString();


            String prompt =
                    "다음은 약봉투 OCR 결과와 추출된 약품명 목록이다.\n" +
                            "아래 규칙에 따라 약을 그룹으로 묶고 JSON으로만 응답하라.\n\n" +

                            "[그룹화 규칙]\n" +
                            "1. 기본 원칙: 같은 봉투에 있는 약은 복용 기간이 명확히 다를 때만 분리하라.\n" +
                            "   - OCR에서 각 약마다 다른 복용 기간이 명시된 경우에만 별도 그룹으로 분리.\n" +
                            "   - 기간 정보가 없거나 불확실하면 모든 약을 하나의 그룹으로 묶어라.\n" +
                            "   예) 코미정/슈다페드정/세토펜정 → 기간 동일 → 하나의 그룹 '감기약'\n" +
                            "   예) 코미정 3일치 + 아목시실린 7일치 → 명확히 다름 → 별도 그룹\n" +
                            "2. 복용 기간이 같고 시간대도 같은 약은 반드시 하나의 그룹으로 묶어라.\n" +
                            "3. 복용시간이 여러 개면 intakeTimes 배열에 모두 포함하라.\n" +
                            "4. 취침 전 계열 약이면 bedtimeGroup=true, intakeTimes에 저녁 포함.\n" +
                            "5. startDate/endDate 설정 규칙:\n" +
                            "   - OCR에 처방일(조제일)이 있으면 그 날짜를 startDate로 사용.\n" +
                            "   - 복용 일수가 있으면 startDate + 일수로 endDate 계산.\n" +
                            "   - 예) 조제일 2026-06-01, 3일분\n" +
                            "         → startDate: 2026-06-01, endDate: 2026-06-03\n" +
                            "   - 날짜 정보가 전혀 없으면 오늘 날짜(" + getTodayString() + ")를 startDate로 사용.\n" +
                            "   - endDate를 알 수 없으면 startDate + 3일로 기본 설정.\n" +
                            "6. groupTitle은 효능군(감기약, 혈압약 등)으로 짓되,\n" +
                            "   기간이 달라 분리된 경우에만 약품명을 사용하라.\n" +
                            "7. 【복용 횟수】 OCR에서 '1회 2정', '아침 2알' 등이 있으면\n" +
                            "   quantity에 숫자로, unit에 단위(정/알/캡슐)를 설정하라.\n" +
                            "   확실하지 않으면 quantity=1, unit=정 으로 두어라.\n\n" +

                            "[OCR rawText]\n" + rawText + "\n\n" +
                            "[drugNames]\n" + drugListBuilder + "\n\n" +

                            "출력 형식 (JSON만, 설명 없이):\n" +
                            "{\n" +
                            "  \"groups\": [\n" +
                            "    {\n" +
                            "      \"groupTitle\": \"감기약\",\n" +
                            "      \"drugNames\": [\"코미정\", \"슈다페드정\", \"세토펜정325밀리그램\"],\n" +
                            "      \"startDate\": \"2026-06-01\",\n" +
                            "      \"endDate\": \"2026-06-03\",\n" +
                            "      \"intakeTimes\": [\"아침\", \"점심\", \"저녁\"],\n" +
                            "      \"bedtimeGroup\": false,\n" +
                            "      \"quantity\": 1,\n" +
                            "      \"unit\": \"정\"\n" +
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

            // intakeTimes 배열 파싱
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
            item.setQuantity(obj.optDouble("quantity", 1.0));
            item.setUnit(obj.optString("unit", "정"));


            result.add(item);
        }

        return result;
    }
}
