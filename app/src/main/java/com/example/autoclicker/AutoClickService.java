package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class AutoClickService extends AccessibilityService {
    private static AutoClickService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private float clickX = 500, clickY = 500;
    private long interval = 1000;
    private long maxClicks = 0;
    private long performedClicks = 0;

    private final Runnable clickRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;

            if (maxClicks > 0 && performedClicks >= maxClicks) {
                stopClicking();
                return;
            }

            performClick(clickX, clickY);
            performedClicks++;
            handler.postDelayed(this, interval);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { stopClicking(); }

    public static AutoClickService getInstance() { return instance; }

    public void setClickPosition(float x, float y) {
        clickX = x;
        clickY = y;
    }

    public void setInterval(long milliseconds) {
        interval = Math.max(50, milliseconds);
    }

    public void setMaxClicks(long count) {
        maxClicks = Math.max(0, count);
    }

    public void startClicking() {
        if (running) return;
        running = true;
        performedClicks = 0;
        handler.removeCallbacks(clickRunnable);
        handler.post(clickRunnable);
    }

    public void stopClicking() {
        running = false;
        handler.removeCallbacks(clickRunnable);
    }

    private void performClick(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 50);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
    }
}
