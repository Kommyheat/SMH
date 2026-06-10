package com.app.smh.calendar;

public class CalendarDayItem {

    private String dateString;
    private int dayNumber;
    private boolean currentMonth;
    private boolean today;
    private boolean done;
    private boolean selected;

    public CalendarDayItem(String dateString, int dayNumber, boolean currentMonth, boolean today, boolean done, boolean selected) {
        this.dateString = dateString;
        this.dayNumber = dayNumber;
        this.currentMonth = currentMonth;
        this.today = today;
        this.done = done;
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

    public boolean isSelected() {
        return selected;
    }
}