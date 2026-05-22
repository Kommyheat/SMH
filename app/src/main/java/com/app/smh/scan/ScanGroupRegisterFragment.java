package com.app.smh.scan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.smh.MainActivity;
import com.app.smh.R;
import com.app.smh.schedule.MedicationServerSync;
import com.app.smh.schedule.ScheduleMedicineItem;
import com.app.smh.schedule.ScheduleRepository;
import com.app.smh.schedule.MedicationServerSync;

import java.util.ArrayList;
import java.util.List;

public class ScanGroupRegisterFragment extends Fragment {

    private static final String ARG_DRUG_NAMES = "arg_drug_names";
    private static final String ARG_RAW_TEXT = "arg_raw_text";

    private RecyclerView recyclerGroups;
    private Button btnRegisterAll;

    private ArrayList<String> drugNames;
    private String rawText;

    private final ArrayList<ScanAnalysisGroupItem> groupItems = new ArrayList<>();
    private ScanGroupAdapter adapter;

    public ScanGroupRegisterFragment() {
        super(R.layout.fragment_scan_group_register);
    }

    public static ScanGroupRegisterFragment newInstance(ArrayList<String> drugNames, String rawText) {
        ScanGroupRegisterFragment fragment = new ScanGroupRegisterFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_DRUG_NAMES, drugNames);
        args.putString(ARG_RAW_TEXT, rawText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerGroups = view.findViewById(R.id.recycler_groups);
        btnRegisterAll = view.findViewById(R.id.btn_register_all);

        recyclerGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ScanGroupAdapter(requireContext(), groupItems, this::updateRegisterButtonState);
        recyclerGroups.setAdapter(adapter);

        getArgumentsData();
        loadGeminiGroupAnalysis();

        btnRegisterAll.setOnClickListener(v -> registerAllGroups());
    }

    private void getArgumentsData() {
        Bundle args = getArguments();
        if (args != null) {
            drugNames = args.getStringArrayList(ARG_DRUG_NAMES);
            rawText = args.getString(ARG_RAW_TEXT, "");
        }

        if (drugNames == null) drugNames = new ArrayList<>();
        if (rawText == null) rawText = "";

        Log.d("ScanGroupRegister", "drugNames = " + drugNames);
        Log.d("ScanGroupRegister", "rawText = " + rawText);
    }

    private void loadGeminiGroupAnalysis() {
        if (drugNames == null || drugNames.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "전달된 약품명이 없습니다.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        GeminiDrugGroupManager manager = new GeminiDrugGroupManager();
        manager.analyzeDrugGroups(drugNames, rawText, new GeminiDrugGroupManager.GroupCallback() {
            @Override
            public void onSuccess(ArrayList<ScanAnalysisGroupItem> groups) {
                if (!isAdded()) return;

                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    if (adapter == null) return;

                    ArrayList<ScanAnalysisGroupItem> safeGroups = new ArrayList<>();

                    if (groups != null) {
                        for (ScanAnalysisGroupItem item : groups) {
                            if (item == null) continue;
                            if (item.getGroupTitle() == null) item.setGroupTitle("복용약");
                            if (item.getDrugNames() == null) item.setDrugNames(new ArrayList<>());
                            if (item.getStartDate() == null) item.setStartDate("");
                            if (item.getEndDate() == null) item.setEndDate("");
                            // 수정: intakeTimes null 체크
                            if (item.getIntakeTimes() == null) item.setIntakeTimes(new ArrayList<>());
                            safeGroups.add(item);
                        }
                    }

                    groupItems.clear();
                    groupItems.addAll(safeGroups);

                    Log.d("ScanGroupRegister", "groupItems size = " + groupItems.size());

                    adapter.notifyDataSetChanged();
                    updateRegisterButtonState();

                    if (groupItems.isEmpty()) {
                        Toast.makeText(getContext(), "분석 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(
                            getContext(),
                            message != null ? message : "약그룹 분석에 실패했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                    updateRegisterButtonState();
                });
            }
        });
    }

    private void registerAllGroups() {
        if (!isAdded() || getContext() == null) return;

        if (groupItems.isEmpty()) {
            Toast.makeText(getContext(), "등록할 복약 그룹이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<ScheduleMedicineItem> scheduleItems = new ArrayList<>();

        for (ScanAnalysisGroupItem item : groupItems) {
            if (item == null || !item.isValid()) {
                Toast.makeText(getContext(), "입력이 완료되지 않은 그룹이 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 캐시에서 약품별 상세정보 구성
            List<ScheduleMedicineItem.DrugDetail> details = buildDrugDetails(item);

            // 수정: 선택된 시간대마다 각각 ScheduleMedicineItem 생성
            for (String timeSlot : item.getIntakeTimes()) {
                ScheduleMedicineItem scheduleItem = new ScheduleMedicineItem();
                scheduleItem.setCategoryName(item.getGroupTitle());
                scheduleItem.setDrugNames(item.getDrugNames());
                scheduleItem.setStartDate(item.getStartDate());
                scheduleItem.setEndDate(item.getEndDate());
                scheduleItem.setTimeSlot(timeSlot);
                scheduleItem.setCompleted(false);
                scheduleItem.setDrugDetails(details);
                scheduleItems.add(scheduleItem);
            }
        }

        ScheduleRepository.addSchedules(requireContext(), scheduleItems);

        // 추가: 서버 저장 (비동기, 실패해도 영향 없음)
        MedicationServerSync.syncToServer(requireContext(), scheduleItems);

        // 등록 완료 후 캐시 초기화
        DrugDetailCache.getInstance().clear();

        Toast.makeText(getContext(), "복약 스케줄이 등록되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // 캐시에서 약품 상세정보 구성 (별도 메서드)
    private List<ScheduleMedicineItem.DrugDetail> buildDrugDetails(ScanAnalysisGroupItem item) {
        List<ScheduleMedicineItem.DrugDetail> details = new ArrayList<>();
        DrugDetailCache cache = DrugDetailCache.getInstance();

        for (String drugName : item.getDrugNames()) {
            DrugResultItem cached = cache.get(drugName);
            if (cached != null && cached.hasDetail()) {
                ScheduleMedicineItem.DrugDetail detail = new ScheduleMedicineItem.DrugDetail();
                detail.recognizedName = drugName;
                detail.itemName = cached.getItemName();
                detail.entpName = cached.getEntpName();
                detail.efcyQesitm = cached.getEfcyQesitm();
                detail.useMethodQesitm = cached.getUseMethodQesitm();
                detail.atpnWarnQesitm = cached.getAtpnWarnQesitm();
                detail.depositMethodQesitm = cached.getDepositMethodQesitm();
                details.add(detail);
            }
        }
        return details;
    }

    private void updateRegisterButtonState() {
        if (btnRegisterAll == null) return;

        boolean allValid = !groupItems.isEmpty();
        for (ScanAnalysisGroupItem item : groupItems) {
            if (item == null || !item.isValid()) {
                allValid = false;
                break;
            }
        }

        btnRegisterAll.setEnabled(allValid);
        btnRegisterAll.setAlpha(allValid ? 1f : 0.4f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerGroups != null) recyclerGroups.setAdapter(null);
        adapter = null;
        recyclerGroups = null;
        btnRegisterAll = null;
    }
}
