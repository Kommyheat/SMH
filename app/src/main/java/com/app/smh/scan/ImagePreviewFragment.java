package com.app.smh.scan;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.smh.R;

public class ImagePreviewFragment extends Fragment {

    private static final String ARG_IMAGE_URI = "arg_image_uri";
    private static final String ARG_SOURCE = "arg_source";

    private Uri imageUri;
    private String source;
    private GeminiOcrManager geminiOcrManager;

    private View loadingOverlay;
    private Button btnAnalyze;

    public ImagePreviewFragment() {
        super(R.layout.fragment_image_preview);
    }

    public static ImagePreviewFragment newInstance(String source, String imageUriString) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOURCE, source);
        args.putString(ARG_IMAGE_URI, imageUriString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        geminiOcrManager = new GeminiOcrManager();

        Bundle args = getArguments();
        if (args != null) {
            source = args.getString(ARG_SOURCE);

            String uriString = args.getString(ARG_IMAGE_URI);
            if (uriString != null) {
                imageUri = Uri.parse(uriString);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imagePreview = view.findViewById(R.id.image_preview);
        Button btnReselect = view.findViewById(R.id.btn_reselect);
        btnAnalyze = view.findViewById(R.id.btn_analyze);
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        if (imageUri != null) {
            imagePreview.setImageURI(imageUri);
        }

        btnReselect.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnAnalyze.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(requireContext(), "이미지가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);

            geminiOcrManager.analyzeImage(
                    requireContext(),
                    source != null ? source : "unknown",
                    imageUri,
                    null,
                    new GeminiOcrManager.OcrCallback() {
                        @Override
                        public void onSuccess(OcrResult result) {
                            if (!isAdded()) return;

                            showLoading(false);
                            openScanResultConfirm(result);
                        }

                        @Override
                        public void onError(String message) {
                            if (!isAdded()) return;

                            showLoading(false);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (btnAnalyze != null) {
            btnAnalyze.setEnabled(!show);
            btnAnalyze.setText(show ? "분석 중..." : "이 사진으로 분석");
            btnAnalyze.setAlpha(show ? 0.7f : 1.0f);
        }
    }

    private void openScanResultConfirm(OcrResult result) {
        ScanResultConfirmFragment fragment = ScanResultConfirmFragment.newInstance(result);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}