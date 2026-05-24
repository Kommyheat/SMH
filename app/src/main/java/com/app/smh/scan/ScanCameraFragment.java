package com.app.smh.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.app.smh.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ScanCameraFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private View loadingOverlay;
    private ImageCapture imageCapture;
    private ActivityResultLauncher<String> pickImageLauncher;

    public ScanCameraFragment() {
        super(R.layout.fragment_scan_camera);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processSelectedImage("gallery", uri, null);
                    } else {
                        Toast.makeText(requireContext(), "이미지 선택 취소", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        requireActivity().getWindow().setStatusBarColor(android.graphics.Color.BLACK);

        View topAppBar = requireActivity().findViewById(R.id.top_app_bar);
        if (topAppBar != null) {
            topAppBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        View topAppBar = requireActivity().findViewById(R.id.top_app_bar);
        if (topAppBar != null) {
            topAppBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        ImageButton btnCapture = view.findViewById(R.id.btn_capture);
        ImageButton btnPickImage = view.findViewById(R.id.btn_pick_image);

        btnCapture.setOnClickListener(v -> takePhoto());
        btnPickImage.setOnClickListener(v -> openGallery());

        if (hasCameraPermission()) {
            startCameraPreview();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCameraPreview() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                if (!isAdded() || getView() == null || previewView == null) {
                    return;
                }

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "카메라 시작 실패", Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "카메라가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoDir = getPhotoDirectory();
        if (!photoDir.exists()) {
            photoDir.mkdirs();
        }

        String fileName = "SCAN_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".jpg";

        File photoFile = new File(photoDir, fileName);

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        showLoading(true);

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        showLoading(false);
                        Uri savedUri = Uri.fromFile(photoFile);
                        processSelectedImage("camera", savedUri, photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        showLoading(false);
                        exception.printStackTrace();
                        Toast.makeText(requireContext(), "사진 촬영 실패", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private File getPhotoDirectory() {
        File baseDir = requireContext().getExternalFilesDir("scan_images");
        if (baseDir != null) {
            return baseDir;
        }
        return requireContext().getFilesDir();
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }


     // 카메라 촬영 / 갤러리 선택 결과를 한 군데로 모아서 처리하는 함수
    private void processSelectedImage(String source, Uri imageUri, @Nullable File imageFile) {
        showLoading(false);

        if (imageUri == null) {
            Toast.makeText(requireContext(), "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        openImagePreview(source, imageUri);
    }

    private void openImagePreview(String source, Uri imageUri) {
        ImagePreviewFragment fragment =
                ImagePreviewFragment.newInstance(source, imageUri.toString());

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
