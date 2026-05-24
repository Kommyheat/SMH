package com.app.smh;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREF_NAME = "smh_settings";

    private static final String KEY_HOME_MESSAGE = "key_home_message";
    private static final String DEFAULT_HOME_MESSAGE = "오늘도 건강한 하루를 지켜요.";

    private static final String KEY_DEV_AUTO_LOGIN = "key_dev_auto_login";
    private static final String KEY_AUTO_LOGIN_ID = "key_auto_login_id";
    private static final String KEY_AUTO_LOGIN_PASSWORD = "key_auto_login_password";

    private static final String KEY_LOGIN_USER_ID = "key_login_user_id";
    private static final String KEY_LOGIN_USER_LOGIN_ID = "key_login_user_login_id";
    private static final String KEY_LOGIN_USER_NAME = "key_login_user_name";

    private static final String KEY_BIRTH_DATE = "key_birth_date";
    private static final String KEY_EMAIL = "key_email";

    private static final String KEY_DARK_MODE = "key_dark_mode";
    private static final String KEY_TTS_ENABLED = "key_tts_enabled";
    private static final String KEY_SENIOR_MODE = "key_senior_mode";

    // 알림 시간 설정 키
    private static final String KEY_ALARM_MORNING_TIME = "key_alarm_morning_time";
    private static final String KEY_ALARM_LUNCH_TIME = "key_alarm_lunch_time";
    private static final String KEY_ALARM_DINNER_TIME = "key_alarm_dinner_time";
    private static final String KEY_ALARM_WAKEUP_TIME = "key_alarm_wakeup_time";
    private static final String KEY_ALARM_BEDTIME_TIME = "key_alarm_bedtime_time";

    // 선택 알림 활성화 여부
    private static final String KEY_ALARM_WAKEUP_ENABLED = "key_alarm_wakeup_enabled";
    private static final String KEY_ALARM_BEDTIME_ENABLED = "key_alarm_bedtime_enabled";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveHomeMessage(Context context, String message) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(KEY_HOME_MESSAGE, message).apply();
    }

    public static String getHomeMessage(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_HOME_MESSAGE, DEFAULT_HOME_MESSAGE);
    }

    public static void setDeveloperAutoLogin(Context context, boolean enabled) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(KEY_DEV_AUTO_LOGIN, enabled).apply();
    }

    public static boolean isDeveloperAutoLogin(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(KEY_DEV_AUTO_LOGIN, false);
    }

    public static void clearDeveloperAutoLogin(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().remove(KEY_DEV_AUTO_LOGIN).apply();
    }

    public static void saveAutoLoginCredentials(Context context, String loginId, String password) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putString(KEY_AUTO_LOGIN_ID, loginId)
                .putString(KEY_AUTO_LOGIN_PASSWORD, password)
                .apply();
    }

    public static String getAutoLoginId(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_AUTO_LOGIN_ID, "");
    }

    public static String getAutoLoginPassword(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_AUTO_LOGIN_PASSWORD, "");
    }

    public static boolean hasAutoLoginCredentials(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String id = prefs.getString(KEY_AUTO_LOGIN_ID, "");
        String password = prefs.getString(KEY_AUTO_LOGIN_PASSWORD, "");
        return id != null && !id.trim().isEmpty() && password != null && !password.trim().isEmpty();
    }

    public static void clearAutoLoginCredentials(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .remove(KEY_AUTO_LOGIN_ID)
                .remove(KEY_AUTO_LOGIN_PASSWORD)
                .remove(KEY_DEV_AUTO_LOGIN)
                .apply();
    }

    public static void saveLoginSession(Context context, long userId, String loginId, String name) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putLong(KEY_LOGIN_USER_ID, userId)
                .putString(KEY_LOGIN_USER_LOGIN_ID, loginId)
                .putString(KEY_LOGIN_USER_NAME, name)
                .apply();
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.contains(KEY_LOGIN_USER_ID);
    }

    public static String getLoginUserName(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_LOGIN_USER_NAME, "");
    }

    public static long getLoginUserId(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getLong(KEY_LOGIN_USER_ID, -1L);
    }

    public static void clearLoginSession(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .remove(KEY_LOGIN_USER_ID)
                .remove(KEY_LOGIN_USER_LOGIN_ID)
                .remove(KEY_LOGIN_USER_NAME)
                .apply();
    }

    public static void setBirthDate(Context context, String birthDate) {
        getPrefs(context).edit().putString(KEY_BIRTH_DATE, birthDate).apply();
    }

    public static String getBirthDate(Context context) {
        return getPrefs(context).getString(KEY_BIRTH_DATE, "-");
    }

    public static void setEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "-");
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public static void setTtsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply();
    }

    public static boolean isTtsEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(KEY_TTS_ENABLED, false);
    }

    public static void setSeniorModeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(KEY_SENIOR_MODE, enabled).apply();
    }

    public static boolean isSeniorModeEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(KEY_SENIOR_MODE, false);
    }


    // 알림 설정
    public static String getAlarmMorningTime(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_ALARM_MORNING_TIME, null);
    }

    public static void setAlarmMorningTime(Context context, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(KEY_ALARM_MORNING_TIME, value).apply();
    }

    public static String getAlarmLunchTime(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_ALARM_LUNCH_TIME, null);
    }

    public static void setAlarmLunchTime(Context context, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(KEY_ALARM_LUNCH_TIME, value).apply();
    }

    public static String getAlarmDinnerTime(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_ALARM_DINNER_TIME, null);
    }

    public static void setAlarmDinnerTime(Context context, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(KEY_ALARM_DINNER_TIME, value).apply();
    }

    public static String getAlarmWakeupTime(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_ALARM_WAKEUP_TIME, "07:00");
    }

    public static void setAlarmWakeupTime(Context context, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(KEY_ALARM_WAKEUP_TIME, value).apply();
    }

    public static String getAlarmBedtimeTime(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(KEY_ALARM_BEDTIME_TIME, "21:30");
    }

    public static void setAlarmBedtimeTime(Context context, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(KEY_ALARM_BEDTIME_TIME, value).apply();
    }

    public static boolean isWakeupAlarmEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(KEY_ALARM_WAKEUP_ENABLED, false);
    }

    public static void setWakeupAlarmEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(KEY_ALARM_WAKEUP_ENABLED, enabled).apply();
    }

    public static boolean isBedtimeAlarmEnabled(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getBoolean(KEY_ALARM_BEDTIME_ENABLED, false);
    }

    public static void setBedtimeAlarmEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(KEY_ALARM_BEDTIME_ENABLED, enabled).apply();
    }

    // 글자 크기 설정 (1.0f = 기본, 1.3f = 큰 글씨)
    private static final String KEY_FONT_SCALE = "key_font_scale";

    public static float getFontScale(Context context) {
        return getPrefs(context).getFloat(KEY_FONT_SCALE, 1.0f);
    }

    public static void setFontScale(Context context, float scale) {
        getPrefs(context).edit().putFloat(KEY_FONT_SCALE, scale).apply();
    }

    // 프로필 이미지 경로 저장
    private static final String KEY_PROFILE_IMAGE_PATH = "key_profile_image_path";

    public static void setProfileImagePath(Context context, String path) {
        if (path == null) {
            // null이면 키 제거
            getPrefs(context).edit().remove(KEY_PROFILE_IMAGE_PATH).apply();
        } else {
            getPrefs(context).edit().putString(KEY_PROFILE_IMAGE_PATH, path).apply();
        }
    }

    public static String getProfileImagePath(Context context) {
        return getPrefs(context).getString(KEY_PROFILE_IMAGE_PATH, null);
    }

    // timeSlot에 따라 알림 설정 시간 자동 적용
    private static String getScheduledTime(Context context, String timeSlot) {
        switch (timeSlot) {
            case "아침":
                String morning = SettingsManager.getAlarmMorningTime(context);
                return morning != null ? morning + ":00" : "08:00:00";
            case "점심":
                String lunch = SettingsManager.getAlarmLunchTime(context);
                return lunch != null ? lunch + ":00" : "12:00:00";
            case "저녁":
                String dinner = SettingsManager.getAlarmDinnerTime(context);
                return dinner != null ? dinner + ":00" : "18:00:00";
            case "취침 전":
                String bedtime = SettingsManager.getAlarmBedtimeTime(context);
                return bedtime != null ? bedtime + ":00" : "21:30:00";
            default:
                return "00:00:00";
        }
    }
    // 보호자 내용 공유 관련
    private static final String KEY_SHARE_STATUS = "key_share_status";
    private static final String KEY_MISSED_ALERT = "key_missed_alert";

    public static void setShareStatusEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_SHARE_STATUS, enabled).apply();
    }
    public static boolean isShareStatusEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_SHARE_STATUS, true);
    }

    public static void setMissedAlertEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_MISSED_ALERT, enabled).apply();
    }
    public static boolean isMissedAlertEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_MISSED_ALERT, false);
    }
}
