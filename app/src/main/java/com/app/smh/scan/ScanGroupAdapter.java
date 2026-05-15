package com.app.smh.scan;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.app.smh.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ScanGroupAdapter extends RecyclerView.Adapter<ScanGroupAdapter.GroupViewHolder> {

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    private final Context context;
    private final ArrayList<ScanAnalysisGroupItem> items;
    private final OnDataChangedListener listener;

    public ScanGroupAdapter(Context context, ArrayList<ScanAnalysisGroupItem> items,
                            OnDataChangedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_scan_group_register, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        ScanAnalysisGroupItem item = items.get(position);

        if (item == null) {
            Log.e("ScanGroupAdapter", "item is null at position " + position);
            return;
        }

        // 그룹 제목
        holder.tvGroupTitle.setText(
                item.getGroupTitle() != null ? item.getGroupTitle() : "복용약");

        // 약품명
        holder.tvDrugNames.setText(
                item.getDrugNamesText() != null ? item.getDrugNamesText() : "약 정보 없음");

        // 시작일
        String startDate = item.getStartDate();
        holder.tvStartDate.setText(
                startDate != null && !startDate.isEmpty() ? startDate : "시작일 선택");

        // 종료일
        String endDate = item.getEndDate();
        holder.tvEndDate.setText(
                endDate != null && !endDate.isEmpty() ? endDate : "종료일 선택");

        // 취침 전 뱃지
        holder.tvBedtimeBadge.setVisibility(
                item.isBedtimeGroup() ? View.VISIBLE : View.GONE);

        // 수정: 리스너 먼저 해제 후 다중 선택 상태 반영
        holder.chipGroupTime.setOnCheckedStateChangeListener(null);
        holder.chipGroupTime.clearCheck();

        ArrayList<String> intakeTimes = item.getIntakeTimes();
        if (intakeTimes.contains("아침")) holder.chipGroupTime.check(R.id.chip_morning);
        if (intakeTimes.contains("점심")) holder.chipGroupTime.check(R.id.chip_lunch);
        if (intakeTimes.contains("저녁")) holder.chipGroupTime.check(R.id.chip_dinner);

        // 날짜 선택 리스너
        holder.tvStartDate.setOnClickListener(v ->
                showDatePicker(holder.tvStartDate, date -> {
                    item.setStartDate(date);
                    if (listener != null) listener.onDataChanged();
                })
        );

        holder.tvEndDate.setOnClickListener(v ->
                showDatePicker(holder.tvEndDate, date -> {
                    item.setEndDate(date);
                    if (listener != null) listener.onDataChanged();
                })
        );

        // 수정: 다중 선택 리스너
        holder.chipGroupTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
            ArrayList<String> selected = new ArrayList<>();

            Chip chipMorning = group.findViewById(R.id.chip_morning);
            Chip chipLunch = group.findViewById(R.id.chip_lunch);
            Chip chipDinner = group.findViewById(R.id.chip_dinner);

            if (chipMorning != null && chipMorning.isChecked()) selected.add("아침");
            if (chipLunch != null && chipLunch.isChecked()) selected.add("점심");
            if (chipDinner != null && chipDinner.isChecked()) selected.add("저녁");

            item.setIntakeTimes(selected);

            if (listener != null) listener.onDataChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void showDatePicker(TextView target, OnDateSelectedListener dateSelectedListener) {
        if (!(context instanceof FragmentActivity)) {
            Toast.makeText(context, "날짜 선택을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("날짜 선택")
                .setSelection(Calendar.getInstance().getTimeInMillis())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            Calendar selected = Calendar.getInstance();
            selected.setTimeInMillis(selection);
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    selected.get(Calendar.YEAR),
                    selected.get(Calendar.MONTH) + 1,
                    selected.get(Calendar.DAY_OF_MONTH));
            target.setText(date);
            dateSelectedListener.onSelected(date);
        });

        picker.show(((FragmentActivity) context).getSupportFragmentManager(),
                "scan_group_date_picker");
    }

    public ArrayList<ScanAnalysisGroupItem> getItems() {
        return items;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupTitle, tvDrugNames, tvStartDate, tvEndDate, tvBedtimeBadge;
        ChipGroup chipGroupTime;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupTitle = itemView.findViewById(R.id.tv_group_title);
            tvDrugNames = itemView.findViewById(R.id.tv_drug_names);
            tvStartDate = itemView.findViewById(R.id.tv_start_date);
            tvEndDate = itemView.findViewById(R.id.tv_end_date);
            tvBedtimeBadge = itemView.findViewById(R.id.tv_bedtime_badge);
            chipGroupTime = itemView.findViewById(R.id.chip_group_time);
        }
    }

    interface OnDateSelectedListener {
        void onSelected(String date);
    }
}
