package com.app.smh;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SettingsManager.wrapContextWithFontScale(newBase));
    }
}
