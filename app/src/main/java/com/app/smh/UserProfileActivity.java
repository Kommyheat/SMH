package com.app.smh;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.app.smh.auth.AuthApiClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvProfileName;
    private TextView tvProfileBirth;
    private TextView tvProfileEmail;
    private TextView tvProfileCode;

    private AuthApiClient authApiClient;
    private long currentUserId = -1L;
    private Uri cameraImageUri;
    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_GALLERY = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        authApiClient = new AuthApiClient();
        currentUserId = SettingsManager.getLoginUserId(this);

        initViews();
        loadSavedProfileImage();
        loadProfileData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileBirth = findViewById(R.id.tv_profile_birth);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileCode = findViewById(R.id.tv_profile_code);

        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btn_change_photo).setOnClickListener(v -> showPhotoDialog());
    }

    // uCrop 시작
    private void startCrop(Uri sourceUri) {
        // 크롭 결과 저장 파일
        Uri destinationUri = Uri.fromFile(
                new File(getCacheDir(), "cropped_profile.jpg"));

        UCrop.of(sourceUri, destinationUri)
                // 1:1 정사각형 고정
                .withAspectRatio(1, 1)
                // 최대 출력 크기
                .withMaxResultSize(512, 512)
                // 원형 오버레이
                .withOptions(getCropOptions())
                .start(this);
    }

    private UCrop.Options getCropOptions() {
        UCrop.Options options = new UCrop.Options();
        // 원형 크롭 오버레이
        options.setCircleDimmedLayer(true);
        // 툴바 색상
        options.setToolbarColor(getColor(R.color.main_coral));
        options.setToolbarColor(getColor(R.color.main_coral));
        options.setToolbarWidgetColor(getColor(android.R.color.white));
        // 툴바 타이틀
        options.setToolbarTitle("프로필 사진 조정");
        // 자유 비율 비활성화 (1:1 고정)
        options.setFreeStyleCropEnabled(false);
        return options;
    }

    // uCrop 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CAMERA) {
            // 카메라 촬영 완료 → uCrop으로 크롭
            if (cameraImageUri != null) {
                startCrop(cameraImageUri);
            }

        } else if (requestCode == REQUEST_GALLERY) {
            // 갤러리 선택 완료 → uCrop으로 크롭
            if (data != null && data.getData() != null) {
                startCrop(data.getData());
            }

        } else if (requestCode == UCrop.REQUEST_CROP) {
            // uCrop 완료 → 저장
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {
                saveAndApplyProfileImage(croppedUri);
            }


        } else if (resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "이미지 자르기에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 크롭된 이미지 내부 저장소에 저장 + 적용
    private void saveAndApplyProfileImage(Uri uri) {
        new Thread(() -> {
            try {
                File destFile = new File(getFilesDir(), "profile.jpg");
                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(destFile);

                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                inputStream.close();
                outputStream.close();

                // 경로 저장
                SettingsManager.setProfileImagePath(this, destFile.getAbsolutePath());

                runOnUiThread(() -> {
                    // 프로필 이미지 즉시 갱신
                    ivProfileImage.setImageURI(null);
                    ivProfileImage.setImageURI(Uri.fromFile(destFile));
                    Toast.makeText(this, "프로필 사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // 저장된 프로필 이미지 불러오기
    private void loadSavedProfileImage() {
        String savedPath = SettingsManager.getProfileImagePath(this);
        if (savedPath != null) {
            File file = new File(savedPath);
            if (file.exists()) {
                ivProfileImage.setImageURI(Uri.fromFile(file));
            }
        }
    }

    private void loadProfileData() {
        // 이름 (로컬에 저장된 값 즉시 표시)
        String name = SettingsManager.getLoginUserName(this);
        if (name != null && !name.isEmpty()) {
            tvProfileName.setText(name);
        }

        if (currentUserId <= 0) return;

        // 서버에서 상세 정보 조회
        new Thread(() -> {
            try {
                AuthApiClient.UserProfileResponse profile =
                        authApiClient.getUserProfile(currentUserId);

                AuthApiClient.LinkCodeResponse linkCode =
                        authApiClient.getMyLinkCode(currentUserId);

                runOnUiThread(() -> {
                    if (profile != null) {
                        tvProfileName.setText(
                                profile.name != null ? profile.name : "-");
                        tvProfileBirth.setText(
                                profile.birthDate != null ? profile.birthDate : "-");
                        tvProfileEmail.setText(
                                profile.email != null ? profile.email : "-");
                    }
                    if (linkCode != null && linkCode.linkCode != null) {
                        tvProfileCode.setText(linkCode.linkCode);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    // 서버 조회 실패 시 로컬 저장값 표시
                    tvProfileBirth.setText(SettingsManager.getBirthDate(this));
                    tvProfileEmail.setText(SettingsManager.getEmail(this));
                    tvProfileCode.setText("-");
                });
            }
        }).start();
    }

    private void showPhotoDialog() {
        // 현재 저장된 이미지가 있을 때만 "기본 프로필로 변경" 옵션 표시
        String savedPath = SettingsManager.getProfileImagePath(this);
        boolean hasCustomImage = savedPath != null && new File(savedPath).exists();

        String[] options = hasCustomImage
                ? new String[]{"사진 찍기", "사진 보관함에서 가져오기", "기본 프로필로 변경"}
                : new String[]{"사진 찍기", "사진 보관함에서 가져오기"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("프로필 사진 변경")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        takePhoto();
                    } else if (which == 1) {
                        openGallery();
                    } else if (which == 2) {
                        // 기본 프로필로 변경
                        resetToDefaultProfile();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void resetToDefaultProfile() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("기본 프로필로 변경")
                .setMessage("프로필 사진을 기본 이미지로 되돌리시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
                    // 저장된 이미지 파일 삭제
                    String savedPath = SettingsManager.getProfileImagePath(this);
                    if (savedPath != null) {
                        File file = new File(savedPath);
                        if (file.exists()) file.delete();
                    }

                    // 경로 초기화
                    SettingsManager.setProfileImagePath(this, null);

                    // 기본 아이콘으로 복원
                    ivProfileImage.setImageResource(R.drawable.ic_user);

                    Toast.makeText(this, "기본 프로필로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    // 카메라 권한 요청
    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
            });

    private void takePhoto() {
        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openCamera() {
        try {
            // 수정: getFilesDir() 사용 (files-path로 등록됨)
            File photoFile = new File(getFilesDir(), "temp_camera.jpg");
            if (photoFile.exists()) photoFile.delete();

            android.util.Log.d("ProfileCamera", "파일 경로: " + photoFile.getAbsolutePath());

            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);

            android.util.Log.d("ProfileCamera", "URI: " + cameraImageUri);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "카메라 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            startActivityForResult(intent, REQUEST_CAMERA);

        } catch (Exception e) {
            android.util.Log.e("ProfileCamera", "오류: " + e.getMessage(), e);
            Toast.makeText(this, "오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
