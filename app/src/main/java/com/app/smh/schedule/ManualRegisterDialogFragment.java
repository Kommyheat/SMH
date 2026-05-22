package com.app.smh.schedule;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.app.smh.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ManualRegisterDialogFragment extends DialogFragment {

    public interface OnRegisteredListener {
        void onRegistered();
    }

    private OnRegisteredListener listener;

    private TextView tvStartDate;
    private TextView tvEndDate;

    private String selectedStartDate = "";
    private String selectedEndDate = "";

    public static ManualRegisterDialogFragment newInstance(OnRegisteredListener listener) {
        ManualRegisterDialogFragment fragment = new ManualRegisterDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_manual_register, null);

        // 뷰 바인딩
        EditText etCategoryName = view.findViewById(R.id.et_category_name);
        EditText etDrugNames = view.findViewById(R.id.et_drug_names);

        // RadioGroup → CheckBox 3개로 교체
        CheckBox cbMorning = view.findViewById(R.id.cb_morning);
        CheckBox cbLunch = view.findViewById(R.id.cb_lunch);
        CheckBox cbDinner = view.findViewById(R.id.cb_dinner);

        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);

        // 날짜 선택
        tvStartDate.setOnClickListener(v ->
                showDatePicker(date -> {
                    selectedStartDate = date;
                    tvStartDate.setText(date);
                })
        );

        tvEndDate.setOnClickListener(v ->
                showDatePicker(date -> {
                    selectedEndDate = date;
                    tvEndDate.setText(date);
                })
        );

        // 버튼 색상 코럴색으로 변경
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("수기 등록")
                .setView(view)
                .setNegativeButton("취소", null)
                .setPositiveButton("등록", null); // 먼저 null로 설정 후 아래서 override

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dlg -> {
            // 취소 버튼 코럴색
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(android.graphics.Color.parseColor("#FF786E"));

            // 등록 버튼 코럴색
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(android.graphics.Color.parseColor("#FF786E"));

            // 등록 버튼 클릭 로직 (다이얼로그 자동 닫힘 방지를 위해 여기서 처리)
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String categoryName = etCategoryName.getText().toString().trim();
                        String drugNamesRaw = etDrugNames.getText().toString().trim();

                        // 다중 선택된 시간대 수집
                        ArrayList<String> selectedTimeSlots = new ArrayList<>();
                        if (cbMorning.isChecked()) selectedTimeSlots.add("아침");
                        if (cbLunch.isChecked()) selectedTimeSlots.add("점심");
                        if (cbDinner.isChecked()) selectedTimeSlots.add("저녁");

                        // 유효성 검사
                        if (categoryName.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "복약 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (selectedStartDate.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "시작일을 선택해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (selectedEndDate.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "종료일을 선택해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (selectedTimeSlots.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "복용 시간을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 약 이름 파싱 (쉼표 구분)
                        ArrayList<String> drugNames = new ArrayList<>();
                        if (!drugNamesRaw.isEmpty()) {
                            String[] split = drugNamesRaw.split(",");
                            for (String name : split) {
                                String trimmed = name.trim();
                                if (!trimmed.isEmpty()) drugNames.add(trimmed);
                            }
                        }

                        // savedItems 리스트 추가 후 서버 동기화
                        ArrayList<ScheduleMedicineItem> savedItems = new ArrayList<>();

                        for (String timeSlot : selectedTimeSlots) {
                            ScheduleMedicineItem item = new ScheduleMedicineItem();
                            item.setCategoryName(categoryName);
                            item.setDrugNames(new ArrayList<>(drugNames));
                            item.setStartDate(selectedStartDate);
                            item.setEndDate(selectedEndDate);
                            item.setTimeSlot(timeSlot);
                            item.setCompleted(false);

                            // 기존: 로컬 저장 (유지)
                            ScheduleRepository.addSchedule(requireContext(), item);
                            savedItems.add(item); // 추가
                        }


                        // 추가: 서버 저장 (비동기, 실패해도 영향 없음)
                        MedicationServerSync.syncToServer(requireContext(), savedItems);

                        Toast.makeText(requireContext(),
                                "복약 일정이 등록되었습니다.", Toast.LENGTH_SHORT).show();

                        if (listener != null) listener.onRegistered();

                        // 다이얼로그 닫기
                        dialog.dismiss();
                    });
        });

        return dialog;
    }

    private void showDatePicker(OnDateSelectedListener dateListener) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("날짜 선택")
                .setSelection(Calendar.getInstance().getTimeInMillis())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selection);
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
            dateListener.onSelected(date);
        });

        picker.show(getParentFragmentManager(), "manual_date_picker");
    }

    interface OnDateSelectedListener {
        void onSelected(String date);
    }
}
