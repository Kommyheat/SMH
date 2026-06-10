package com.app.smh.scan;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.app.smh.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DrugInfoApiManager {

    private static final String TAG = "PublicDataAPI";
    private static final String SERVICE_KEY = BuildConfig.PublicDATA_API_KEY;
    private static final String BASE_URL = "https://apis.data.go.kr/1471000/DrbEasyDrugInfoService/getDrbEasyDrugList";

    public interface DetailCallback {
        void onSuccess(DrugResultItem item);
        void onError(String message);
    }

    public void fetchDrugDetail(String drugName, DetailCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "fetchDrugDetail() called. original drugName = " + drugName);

                List<String> candidates = buildSearchCandidates(drugName);
                Log.d(TAG, "search candidates = " + candidates);

                for (String candidate : candidates) {
                    DrugResultItem result = requestDrugDetail(candidate, drugName);

                    if (result != null && result.hasDetail()) {
                        Log.d(TAG, "matched candidate = " + candidate);
                        postSuccess(callback, result);
                        return;
                    }
                }

                postError(callback, "검색 결과가 없습니다.");

            } catch (Exception e) {
                Log.e(TAG, "Exception while fetching drug detail", e);
                postError(callback, "약 상세정보 조회 실패: " + e.getMessage());
            }
        }).start();
    }

    private DrugResultItem requestDrugDetail(String searchName, String originalDrugName) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        try {
            String encodedName = URLEncoder.encode(searchName, "UTF-8");

            String requestUrl = BASE_URL
                    + "?serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=10"
                    + "&itemName=" + encodedName
                    + "&type=json";

            Log.d(TAG, "Request URL = " + requestUrl);

            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoInput(true);

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "HTTP responseCode = " + responseCode + " / searchName = " + searchName);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            String responseBody = resultBuilder.toString();
            Log.d(TAG, "Raw response = " +
                    (responseBody.length() > 500
                            ? responseBody.substring(0, 500) + "..."
                            : responseBody));

            JSONObject root = new JSONObject(responseBody);
            JSONObject body = root.optJSONObject("body");

            if (body == null) {
                Log.d(TAG, "body is null");
                return null;
            }

            JSONArray items = body.optJSONArray("items");
            if (items == null || items.length() == 0) {
                Log.d(TAG, "items empty for searchName = " + searchName);
                return null;
            }

            JSONObject bestObject = findBestMatch(items, originalDrugName, searchName);
            if (bestObject == null) {
                bestObject = items.optJSONObject(0);
            }

            if (bestObject == null) {
                return null;
            }

            DrugResultItem item = new DrugResultItem(originalDrugName);
            item.setItemName(cleanText(bestObject.optString("itemName")));
            item.setEntpName(cleanText(bestObject.optString("entpName")));
            item.setEfcyQesitm(cleanLongText(bestObject.optString("efcyQesitm")));
            item.setUseMethodQesitm(cleanLongText(bestObject.optString("useMethodQesitm")));
            item.setAtpnWarnQesitm(cleanLongText(bestObject.optString("atpnWarnQesitm")));
            item.setDepositMethodQesitm(cleanLongText(bestObject.optString("depositMethodQesitm")));

            Log.d(TAG, "Parsed itemName = " + item.getItemName());
            Log.d(TAG, "Parsed entpName = " + item.getEntpName());

            return item;

        } catch (Exception e) {
            Log.e(TAG, "requestDrugDetail() error. searchName = " + searchName, e);
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {
            }

            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private JSONObject findBestMatch(JSONArray items, String originalDrugName, String searchName) {
        JSONObject bestObject = null;
        int bestScore = -1;

        String normalizedOriginal = normalizeDrugName(originalDrugName);
        String normalizedSearch = normalizeDrugName(searchName);

        for (int i = 0; i < items.length(); i++) {
            JSONObject obj = items.optJSONObject(i);
            if (obj == null) continue;

            String apiItemName = obj.optString("itemName");
            String normalizedApiName = normalizeDrugName(apiItemName);

            int score = calculateMatchScore(
                    normalizedOriginal,
                    normalizedSearch,
                    normalizedApiName
            );

            Log.d(TAG, "candidate itemName = " + apiItemName + ", score = " + score);

            if (score > bestScore) {
                bestScore = score;
                bestObject = obj;
            }
        }

        return bestObject;
    }

    private int calculateMatchScore(String normalizedOriginal,
                                    String normalizedSearch,
                                    String normalizedApiName) {
        int score = 0;

        if (normalizedApiName.equals(normalizedOriginal)) score += 100;
        if (normalizedApiName.equals(normalizedSearch)) score += 90;

        if (normalizedApiName.contains(normalizedOriginal)) score += 70;
        if (normalizedApiName.contains(normalizedSearch)) score += 60;

        if (normalizedOriginal.contains(normalizedApiName)) score += 50;
        if (normalizedSearch.contains(normalizedApiName)) score += 40;

        String originalBase = removeDoseInfo(normalizedOriginal);
        String searchBase = removeDoseInfo(normalizedSearch);
        String apiBase = removeDoseInfo(normalizedApiName);

        if (!TextUtils.isEmpty(apiBase) && apiBase.equals(originalBase)) score += 30;
        if (!TextUtils.isEmpty(apiBase) && apiBase.equals(searchBase)) score += 25;
        if (!TextUtils.isEmpty(apiBase) && originalBase.contains(apiBase)) score += 15;
        if (!TextUtils.isEmpty(apiBase) && searchBase.contains(apiBase)) score += 10;

        return score;
    }

    private List<String> buildSearchCandidates(String original) {
        Set<String> candidates = new LinkedHashSet<>();

        String safeOriginal = original == null ? "" : original.trim();
        String normalized = normalizeDrugName(safeOriginal);
        String noParen = normalized.replaceAll("\\(.*", "").trim();
        String noDose = removeDoseInfo(normalized).trim();
        String noSpace = normalized.replace(" ", "").trim();

        addCandidate(candidates, safeOriginal);
        addCandidate(candidates, normalized);
        addCandidate(candidates, noParen);
        addCandidate(candidates, noDose);
        addCandidate(candidates, noSpace);

        return new ArrayList<>(candidates);
    }

    private void addCandidate(Set<String> candidates, String value) {
        if (!TextUtils.isEmpty(value) && value.length() >= 2) {
            candidates.add(value);
        }
    }

    private String normalizeDrugName(String input) {
        if (input == null) return "";

        String text = input.trim();

        text = text.replace("\\n", " ");
        text = text.replace("\\r", " ");
        text = text.replaceAll("\\s+", " ");

        // 괄호와 괄호 안 내용 제거
        text = text.replaceAll("\\s*\\([^\\)]*\\)", " ").trim();

        // 단위 보정
        text = text.replaceAll("밀리그$", "밀리그램");
        text = text.replaceAll("밀리그람$", "밀리그램");
        text = text.replaceAll("그람$", "그램");

        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    private String removeDoseInfo(String text) {
        if (text == null) return "";

        String result = text;
        result = result.replaceAll("\\d+(\\.\\d+)?\\s*(밀리그램|그램|마이크로그램|mg|g|mcg)", "");
        result = result.replaceAll("\\s+", " ").trim();
        return result;
    }

    private String cleanText(String src) {
        if (TextUtils.isEmpty(src)) {
            return "-";
        }

        src = src.replace("&nbsp;", " ")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return src.isEmpty() ? "-" : src;
    }

    private String cleanLongText(String src) {
        String cleaned = cleanText(src);

        if ("-".equals(cleaned)) {
            return cleaned;
        }

        int maxLength = 220;
        return cleaned.length() > maxLength
                ? cleaned.substring(0, maxLength) + "..."
                : cleaned;
    }

    private void postSuccess(DetailCallback callback, DrugResultItem item) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(item));
    }

    private void postError(DetailCallback callback, String message) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
    }
}
