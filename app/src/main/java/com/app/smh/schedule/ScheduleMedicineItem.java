package com.app.smh.schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleMedicineItem {

    private String categoryName;
    private ArrayList<String> drugNames;
    private String startDate;
    private String endDate;
    private String timeSlot;

    // 하위 호환용
    private boolean completed;

    // 날짜별 완료 상태 (key: "yyyy-MM-dd", value: true/false)
    private Map<String, Boolean> completedDates;

    // 스캔 시 가져온 약품별 상세정보
    private List<DrugDetail> drugDetails;
    // intake
    private Long scheduleId;

    private String memo;

    public String getMemo() {
        return memo != null ? memo : "";
    }
    public void setMemo(String memo) {
        this.memo = memo;
    }

    public ScheduleMedicineItem() {
        this.scheduleId = null;
        this.categoryName = "";
        this.drugNames = new ArrayList<>();
        this.startDate = "";
        this.endDate = "";
        this.timeSlot = "";
        this.completed = false;
        this.completedDates = new HashMap<>();
        this.drugDetails = new ArrayList<>();
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "";
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public ArrayList<String> getDrugNames() {
        return drugNames != null ? drugNames : new ArrayList<>();
    }
    public void setDrugNames(ArrayList<String> drugNames) {
        this.drugNames = drugNames;
    }

    public String getStartDate() {
        return startDate != null ? startDate : "";
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate != null ? endDate : "";
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getTimeSlot() {
        return timeSlot != null ? timeSlot : "";
    }
    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    // 하위 호환용 유지
    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // 특정 날짜의 완료 여부 반환
    public boolean isCompletedOn(String date) {
        if (completedDates == null || date == null) return false;
        Boolean val = completedDates.get(date);
        return val != null && val;
    }

    // 특정 날짜의 완료 상태 설정
    public void setCompletedOn(String date, boolean isCompleted) {
        if (completedDates == null) completedDates = new HashMap<>();
        if (date != null) completedDates.put(date, isCompleted);
    }

    // completedDates getter/setter (Gson 직렬화용)
    public Map<String, Boolean> getCompletedDates() {
        return completedDates != null ? completedDates : new HashMap<>();
    }
    public void setCompletedDates(Map<String, Boolean> completedDates) {
        this.completedDates = completedDates;
    }

    // drugDetails getter/setter
    public List<DrugDetail> getDrugDetails() {
        return drugDetails != null ? drugDetails : new ArrayList<>();
    }
    public void setDrugDetails(List<DrugDetail> drugDetails) {
        this.drugDetails = drugDetails;
    }

    public boolean isActiveOn(String date) {
        if (date == null || date.trim().isEmpty()) return false;
        if (startDate == null || startDate.trim().isEmpty()) return false;
        if (endDate == null || endDate.trim().isEmpty()) return false;
        return date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0;
    }

    // 약품 상세정보 내부 클래스 (Gson 직렬화 가능)
    public static class DrugDetail {
        public String recognizedName;
        public String itemName;
        public String entpName;
        public String efcyQesitm;
        public String useMethodQesitm;
        public String atpnWarnQesitm;
        public String depositMethodQesitm;

        public DrugDetail() {}

        public boolean hasDetail() {
            return itemName != null && !itemName.isEmpty() && !"-".equals(itemName);
        }
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
}
