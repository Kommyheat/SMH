package com.app.smh.calendar;

public class CalendarDayItem {

    private String dateString;
    private int dayNumber;
    private boolean currentMonth;
    private boolean today;
    private boolean done;

    private boolean missed;    // 미복용 누락 (빨간색용) - 추가
    private boolean inPeriod;  // 복용 기간 (연회색 배경용) - 추가
    private boolean selected;

    public CalendarDayItem(String dateString, int dayNumber, boolean currentMonth, boolean today, boolean done, boolean missed, boolean inPeriod, boolean selected) {
        this.dateString = dateString;
        this.dayNumber = dayNumber;
        this.currentMonth = currentMonth;
        this.today = today;
        this.done = done;
        this.missed = missed;
        this.inPeriod = inPeriod;
        this.selected = selected;
    }

    public String getDateString() {
        return dateString;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public boolean isToday() {
        return today;
    }

    public boolean isDone() {
        return done;
    }
    public boolean isMissed() { return missed; }   // 추가
    public boolean isInPeriod() { return inPeriod; } // 추가

    public boolean isSelected() {
        return selected;
    }
}
