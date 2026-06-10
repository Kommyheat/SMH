package com.app.smh;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SmhApplication extends Application {

    private static SmhApplication instance;

    private final List<WeakReference<Activity>> activities = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activities.add(new WeakReference<>(activity));
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                pruneActivityReferences(activity);
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        });
    }

    public static void refreshUi(Activity sourceActivity) {
        if (instance == null) {
            if (sourceActivity != null && !sourceActivity.isFinishing()) {
                sourceActivity.recreate();
            }
            return;
        }

        sourceActivity.runOnUiThread(() -> {
            List<Activity> snapshot = new ArrayList<>();
            Iterator<WeakReference<Activity>> iterator = instance.activities.iterator();
            while (iterator.hasNext()) {
                Activity activity = iterator.next().get();
                if (activity == null || activity.isDestroyed()) {
                    iterator.remove();
                    continue;
                }
                snapshot.add(activity);
            }

            for (Activity activity : snapshot) {
                if (!activity.isFinishing()) {
                    activity.recreate();
                }
            }
        });
    }

    private void pruneActivityReferences(Activity activityToRemove) {
        Iterator<WeakReference<Activity>> iterator = activities.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next().get();
            if (activity == null || activity == activityToRemove) {
                iterator.remove();
            }
        }
    }
}
