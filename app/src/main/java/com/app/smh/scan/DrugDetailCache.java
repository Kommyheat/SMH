package com.app.smh.scan;

import java.util.HashMap;
import java.util.Map;

public class DrugDetailCache {

    // 싱글톤
    private static DrugDetailCache instance;
    private final Map<String, DrugResultItem> cache = new HashMap<>();

    private DrugDetailCache() {}

    public static DrugDetailCache getInstance() {
        if (instance == null) {
            instance = new DrugDetailCache();
        }
        return instance;
    }

    // 약품명 → 상세정보 저장
    public void put(String drugName, DrugResultItem item) {
        if (drugName != null && item != null) {
            cache.put(drugName.trim(), item);
        }
    }

    // 약품명으로 상세정보 조회
    public DrugResultItem get(String drugName) {
        if (drugName == null) return null;
        return cache.get(drugName.trim());
    }

    // 전체 캐시 조회
    public Map<String, DrugResultItem> getAll() {
        return cache;
    }

    // 캐시 초기화
    public void clear() {
        cache.clear();
    }
}
