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
    private DrugInfoApiManager apiManager = new DrugInfoApiManager();

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

        apiManager = new DrugInfoApiManager();

        initViews(view);
        getArgumentsData();
        setupRecyclerView();
        setupNextButton();
        // 진입 시 약품 자동 조회
        prefetchDrugDetails();

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


     //최대 5개까지 동시 조회 (API 과호출 방지)
    private void prefetchDrugDetails() {
        if (drugNames == null || drugNames.isEmpty()) return;

        int count = 0;
        for (String drugName : drugNames) {
            if (TextUtils.isEmpty(drugName)) continue;

            // 이미 캐시에 있으면 스킵
            if (DrugDetailCache.getInstance().get(drugName) != null) continue;

            // 최대 5개까지만 자동 조회
            if (count >= 5) break;
            count++;

            final String name = drugName;
            apiManager.fetchDrugDetail(name, new DrugInfoApiManager.DetailCallback() {
                @Override
                public void onSuccess(DrugResultItem result) {
                    // 캐시에 저장
                    DrugDetailCache.getInstance().put(name, result);

                    // RecyclerView 갱신 (Fragment 살아있을 때만)
                    if (isAdded() && recyclerView != null) {
                        requireActivity().runOnUiThread(() -> {
                            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                            if (adapter != null) adapter.notifyDataSetChanged();
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    // 자동 조회 실패는 조용히 무시 (사용자에게 표시 안 함)
                    android.util.Log.d("DrugPrefetch",
                            "자동 조회 실패 (무시): " + name + " / " + message);
                }
            });
        }
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
