package com.app.smh.scan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.smh.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ScanResultConfirmFragment extends Fragment {

    private static final String ARG_OCR_RESULT = "arg_ocr_result";

    private OcrResult ocrResult;
    private ArrayList<String> editableDrugNames = new ArrayList<>();

    private LinearLayout layoutDrugList;
    private Button btnRescan;
    private Button btnNext;
    private String rawText;

    public ScanResultConfirmFragment() {
        super(R.layout.fragment_scan_result_confirm);
    }

    public static ScanResultConfirmFragment newInstance(OcrResult result) {
        ScanResultConfirmFragment fragment = new ScanResultConfirmFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_OCR_RESULT, result);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            ocrResult = args.getParcelable(ARG_OCR_RESULT);
        }

        if (ocrResult != null && ocrResult.getDrugNames() != null) {
            editableDrugNames.clear();
            editableDrugNames.addAll(ocrResult.getDrugNames());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutDrugList = view.findViewById(R.id.layout_drug_list);
        btnRescan = view.findViewById(R.id.btn_rescan);
        btnNext = view.findViewById(R.id.btn_next);

        renderDrugList();

        btnRescan.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack(null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });

        btnNext.setOnClickListener(v -> {
            if (editableDrugNames.isEmpty()) {
                Toast.makeText(requireContext(), "인식된 약 이름이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> confirmedDrugNames = new ArrayList<>(editableDrugNames);

            DrugSearchResultFragment fragment =
                    DrugSearchResultFragment.newInstance(confirmedDrugNames, rawText);

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void renderDrugList() {
        layoutDrugList.removeAllViews();

        if (editableDrugNames.isEmpty()) {
            addDrugRow("인식된 약 이름 없음", -1, false);
            return;
        }

        for (int i = 0; i < editableDrugNames.size(); i++) {
            addDrugRow(editableDrugNames.get(i), i, true);
        }
    }

    private void addDrugRow(String drugName, int index, boolean editable) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_scan_drug_name, layoutDrugList, false);

        TextView tvDrugName = row.findViewById(R.id.tv_drug_name);
        tvDrugName.setText(drugName);

        if (editable) {
            row.setOnClickListener(v -> showEditDialog(index));
        } else {
            row.setOnClickListener(null);
        }

        layoutDrugList.addView(row);

        View divider = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        divider.setLayoutParams(params);
        divider.setBackgroundColor(0xFFD9D9D9);
        layoutDrugList.addView(divider);
    }

    private void showEditDialog(int index) {
        if (index < 0 || index >= editableDrugNames.size()) return;

        EditText editText = new EditText(requireContext());
        editText.setText(editableDrugNames.get(index));
        editText.setSelection(editText.getText().length());
        editText.setPadding(40, 30, 40, 30);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("약 이름 수정")
                .setView(editText)
                .setPositiveButton("확인", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        editableDrugNames.set(index, newName);
                        renderDrugList();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
