package com.app.smh.calendar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.app.smh.R;

import java.util.ArrayList;
import java.util.Calendar;

public class CalendarGridAdapter extends BaseAdapter {

    public interface OnDayClickListener {
        void onDayClick(CalendarDayItem item);
    }

    private final ArrayList<CalendarDayItem> items;
    private final OnDayClickListener listener;

    private int getDayOfWeek(String dateString) {
        try {
            java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .parse(dateString);
            Calendar calendar = Calendar.getInstance();
            if (date != null) {
                calendar.setTime(date);
                return calendar.get(Calendar.DAY_OF_WEEK);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    public CalendarGridAdapter(ArrayList<CalendarDayItem> items, OnDayClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        }

        TextView tvDay = view.findViewById(R.id.tv_day_number);
        View dotDone = view.findViewById(R.id.view_done_dot);
        CalendarDayItem item = items.get(position);
        Context context = parent.getContext();

        // 1. 다크모드 체크 및 기본 텍스트 색상 결정
        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightMode == Configuration.UI_MODE_NIGHT_YES;
        int defaultTextColor = ContextCompat.getColor(context, isDarkMode ? android.R.color.white : android.R.color.black);

        // 2. 초기화 (재사용 뷰 문제 방지)
        tvDay.setText("");
        tvDay.setBackgroundResource(0);
        tvDay.setTextColor(defaultTextColor);
        dotDone.setVisibility(View.INVISIBLE);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setAlpha(1f);
        view.setEnabled(true);
        view.setOnClickListener(null);

        // 3. 날짜가 없는 공백 칸 처리
        if (item.getDayNumber() <= 0) {
            view.setAlpha(0.35f);
            view.setEnabled(false);
            return view;
        }

        // 4. 날짜 숫자 및 배경 설정
        tvDay.setText(String.valueOf(item.getDayNumber()));

        // [배경색] 처방 기간이면 회색 배경
        if (item.isInPeriod()) {
            view.setBackgroundColor(0xFFF5F5F5);
        }

        // 5. 점(dot) 표시 로직 (우선순위: 완료(초록) > 누락(빨강) > 오늘남음(파랑))
        if (item.isDone()) {
            // [초록 점] 모두 복용 완료
            dotDone.setVisibility(View.VISIBLE);
            dotDone.setBackgroundResource(R.drawable.bg_dot_green);
        }
        else if (item.isMissed()) {
            // [빨간 점] 과거 누락
            dotDone.setVisibility(View.VISIBLE);
            dotDone.setBackgroundResource(R.drawable.bg_dot_red);
        }
        else if (item.isToday() && item.isInPeriod() && !item.isDone()) {
            // [파란 점] 오늘 아직 남음
            dotDone.setVisibility(View.VISIBLE);
            dotDone.setBackgroundResource(R.drawable.bg_dot_blue);
        }

        // 6. 요일별/상태별 텍스트 색상 및 배경 처리
        int dayOfWeek = getDayOfWeek(item.getDateString());

        if (item.isToday()) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_current);
            tvDay.setTextColor(defaultTextColor);
        } else if (item.isSelected()) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_selected);
            tvDay.setTextColor(Color.WHITE);
        } else {
            if (!item.isCurrentMonth()) {
                tvDay.setTextColor(Color.LTGRAY);
            } else if (dayOfWeek == Calendar.SUNDAY) {
                tvDay.setTextColor(0xFFE57373); // 일요일
            } else if (dayOfWeek == Calendar.SATURDAY) {
                tvDay.setTextColor(0xFF5B6EE1); // 토요일
            }
        }

        // 7. 클릭 리스너 설정
        view.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(item);
            }
        });

        return view;
    }
}
