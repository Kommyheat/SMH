package com.app.smh.scan;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.smh.R;

import java.util.ArrayList;
import java.util.List;

public class DrugSearchResultFragment extends Fragment {

    private static final String ARG_DRUG_NAMES = "arg_drug_names";
    private static final String ARG_RAW_TEXT = "arg_raw_text";

    private RecyclerView recyclerView;
    private Button btnNext;

    private ArrayList<String> drugNames;
    private String rawText;

    public DrugSearchResultFragment() {
        super(R.layout.fragment_drug_search_result);
    }

    public static DrugSearchResultFragment newInstance(ArrayList<String> drugNames, String rawText) {
        DrugSearchResultFragment fragment = new DrugSearchResultFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_DRUG_NAMES, drugNames);
        args.putString(ARG_RAW_TEXT, rawText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        getArgumentsData();
        setupRecyclerView();
        setupNextButton();
    }

    private void initViews(@NonNull View view) {
        recyclerView = view.findViewById(R.id.recycler_drug_results);
        btnNext = view.findViewById(R.id.btn_next);
    }

    private void getArgumentsData() {
        Bundle args = getArguments();
        if (args != null) {
            drugNames = args.getStringArrayList(ARG_DRUG_NAMES);
            rawText = args.getString(ARG_RAW_TEXT, "");
        }

        if (drugNames == null) {
            drugNames = new ArrayList<>();
        }

        if (rawText == null) {
            rawText = "";
        }
    }

    private void setupRecyclerView() {
        List<DrugResultItem> itemList = new ArrayList<>();

        for (String name : drugNames) {
            if (!TextUtils.isEmpty(name)) {
                itemList.add(new DrugResultItem(name));
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new DrugResultAdapter(itemList));
    }

    private void setupNextButton() {
        btnNext.setOnClickListener(v -> moveToScanGroupRegister());
    }

    private void moveToScanGroupRegister() {
        if (drugNames == null || drugNames.isEmpty()) {
            Toast.makeText(requireContext(), "등록할 약품명이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ScanGroupRegisterFragment fragment =
                ScanGroupRegisterFragment.newInstance(drugNames, rawText);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}