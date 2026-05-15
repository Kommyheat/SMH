package com.app.smh.scan;

import java.util.ArrayList;

public class ScanAnalysisGroupItem {

    private String groupTitle;
    private ArrayList<String> drugNames;
    private String startDate;
    private String endDate;

    // 수정: 단일 → 다중 시간대
    private ArrayList<String> intakeTimes;
    private boolean bedtimeGroup;

    public ScanAnalysisGroupItem() {
        drugNames = new ArrayList<>();
        intakeTimes = new ArrayList<>();
    }

    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }

    public ArrayList<String> getDrugNames() { return drugNames; }
    public void setDrugNames(ArrayList<String> drugNames) { this.drugNames = drugNames; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    // 추가: 다중 시간대 getter/setter
    public ArrayList<String> getIntakeTimes() {
        return intakeTimes != null ? intakeTimes : new ArrayList<>();
    }
    public void setIntakeTimes(ArrayList<String> intakeTimes) {
        this.intakeTimes = intakeTimes;
    }

    // 기존 호환성 유지: 첫 번째 시간대 반환
    public String getIntakeTime() {
        if (intakeTimes == null || intakeTimes.isEmpty()) return "";
        return intakeTimes.get(0);
    }

    // 기존 호환성 유지: 단일값 설정 시 리스트로 변환
    public void setIntakeTime(String intakeTime) {
        if (intakeTimes == null) intakeTimes = new ArrayList<>();
        intakeTimes.clear();
        if (intakeTime != null && !intakeTime.isEmpty()) {
            intakeTimes.add(intakeTime);
        }
    }

    public boolean isBedtimeGroup() { return bedtimeGroup; }
    public void setBedtimeGroup(boolean bedtimeGroup) { this.bedtimeGroup = bedtimeGroup; }

    // 수정: intakeTimes 기준으로 유효성 검사
    public boolean isValid() {
        return groupTitle != null && !groupTitle.trim().isEmpty()
                && startDate != null && !startDate.trim().isEmpty()
                && endDate != null && !endDate.trim().isEmpty()
                && intakeTimes != null && !intakeTimes.isEmpty();
    }

    public String getDrugNamesText() {
        if (drugNames == null || drugNames.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < drugNames.size(); i++) {
            sb.append(drugNames.get(i));
            if (i < drugNames.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // 추가: 선택된 시간대 텍스트 (예: "아침 / 점심")
    public String getIntakeTimesText() {
        if (intakeTimes == null || intakeTimes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intakeTimes.size(); i++) {
            sb.append(intakeTimes.get(i));
            if (i < intakeTimes.size() - 1) sb.append(" / ");
        }
        return sb.toString();
    }
}
