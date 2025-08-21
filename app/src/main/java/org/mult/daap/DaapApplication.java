package org.mult.daap;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DaapApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                View rootView = activity.getWindow().getDecorView().getRootView();

                ViewCompat.setOnApplyWindowInsetsListener(rootView, (View v, WindowInsetsCompat insets) -> {
                    Insets barsInsets = insets.getInsets(
                            WindowInsetsCompat.Type.statusBars() |
                                    WindowInsetsCompat.Type.navigationBars());
                    v.setPadding(
                            barsInsets.left,
                            barsInsets.top,
                            barsInsets.right,
                            barsInsets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }
        });
    }
}
