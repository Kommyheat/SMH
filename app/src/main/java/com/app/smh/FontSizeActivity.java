package com.app.smh;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FontSizeActivity extends AppCompatActivity {

    private static final float[] FONT_SCALES = {0.85f, 1.0f, 1.15f, 1.3f, 1.5f};

    private TextView tvPreview;
    private TextView tvPreviewStatus;
    private SeekBar seekbarFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_size);

        initViews();
        loadSavedSettings();
        setupListeners();
    }

    private void initViews() {
        tvPreview = findViewById(R.id.tv_preview);
        tvPreviewStatus = findViewById(R.id.tv_preview_status);
        seekbarFont = findViewById(R.id.seekbar_font);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadSavedSettings() {
        float savedScale = SettingsManager.getFontScale(this);
        int progress = scaleToProgress(savedScale);
        seekbarFont.setProgress(progress);
        applyPreview(savedScale);
    }

    private void setupListeners() {
        seekbarFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                applyPreview(FONT_SCALES[progress]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float scale = FONT_SCALES[seekBar.getProgress()];
                SettingsManager.setFontScale(FontSizeActivity.this, scale);
                applyFontScale(scale);
            }
        });
    }

    private void applyPreview(float scale) {
        if (tvPreview != null) tvPreview.setTextSize(16f * scale);
        if (tvPreviewStatus != null) tvPreviewStatus.setTextSize(14f * scale);
    }

    private void applyFontScale(float scale) {
        Configuration config = new Configuration(getResources().getConfiguration());
        config.fontScale = scale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private int scaleToProgress(float scale) {
        for (int i = 0; i < FONT_SCALES.length; i++) {
            if (Math.abs(FONT_SCALES[i] - scale) < 0.01f) return i;
        }
        return 1;
    }
}
