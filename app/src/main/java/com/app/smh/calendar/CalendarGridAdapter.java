package com.app.smh.calendar;

import android.content.Context;
import android.content.res.Configuration;
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

        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        boolean isDarkMode = nightMode == Configuration.UI_MODE_NIGHT_YES;

        tvDay.setText("");
        tvDay.setBackgroundResource(0);
        tvDay.setTextColor(ContextCompat.getColor(
                context,
                isDarkMode ? android.R.color.white : android.R.color.black
        ));
        dotDone.setVisibility(View.INVISIBLE);
        view.setOnClickListener(null);
        view.setEnabled(true);
        view.setAlpha(1f);

        if (item.getDayNumber() <= 0) {
            tvDay.setText("");
            dotDone.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
            view.setAlpha(0.35f);
            return view;
        }

        tvDay.setText(String.valueOf(item.getDayNumber()));

        int dayOfWeek = getDayOfWeek(item.getDateString());

        if (item.isToday()) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_today);
            tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        } else if (item.isSelected()) {
            tvDay.setBackgroundResource(R.drawable.bg_calendar_selected);
            tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            tvDay.setBackgroundResource(0);

            if (!item.isCurrentMonth()) {
                tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            } else if (dayOfWeek == Calendar.SUNDAY) {
                tvDay.setTextColor(0xFFE57373);
            } else if (dayOfWeek == Calendar.SATURDAY) {
                tvDay.setTextColor(0xFF5B6EE1);
            } else {
                tvDay.setTextColor(ContextCompat.getColor(
                        context,
                        isDarkMode ? android.R.color.white : android.R.color.black
                ));
            }
        }


        dotDone.setVisibility(item.isDone() ? View.VISIBLE : View.INVISIBLE);

        view.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(item);
            }
        });

        return view;
    }
}
