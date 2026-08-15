package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.accessibility.AccessibilityEvent;
import java.util.Random;

public class AutoClickService extends AccessibilityService {
    private static AutoClickService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private boolean running;
    private long interval = 500, maxClicks = 0, performedClicks = 0;
    private float x = 500, y = 500;
    private RectF region;
    private WindowManager wm;
    private View floating;
    private WindowManager.LayoutParams floatingParams;
    private View picker;
    private int pickerMode = 0; // 1 point, 2 region
    private float downX, downY;

    private final Runnable clickTask = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (maxClicks > 0 && performedClicks >= maxClicks) { stopClicking(); return; }
            float cx = x, cy = y;
            if (region != null) {
                cx = region.left + random.nextFloat() * Math.max(1, region.width());
                cy = region.top + random.nextFloat() * Math.max(1, region.height());
            }
            performClick(cx, cy);
            performedClicks++;
            handler.postDelayed(this, interval);
            updateFloatingLabel();
        }
    };

    @Override protected void onServiceConnected() { super.onServiceConnected(); instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { stopClicking(); }
    @Override public void onDestroy() { stopClicking(); removeFloating(); if (instance == this) instance = null; super.onDestroy(); }
    public static AutoClickService getInstance() { return instance; }

    public void configureSettings(long ms, long count) {
        interval = Math.max(50, ms); maxClicks = Math.max(0, count);
    }
    public float getSelectedX() { return x; }
    public float getSelectedY() { return y; }
    public RectF getSelectedRegion() { return region == null ? null : new RectF(region); }
    public void setPoint(float px, float py) { x=px; y=py; region=null; }
    public void setRegion(RectF r) { region=r == null ? null : new RectF(r); if (region != null) { x=region.centerX(); y=region.centerY(); } }
    public void startClicking() { if (running) return; running = true; performedClicks = 0; handler.removeCallbacks(clickTask); handler.post(clickTask); updateFloatingLabel(); }
    public void stopClicking() { running = false; handler.removeCallbacks(clickTask); updateFloatingLabel(); }
    public boolean isRunning() { return running; }
    public long getPerformedClicks() { return performedClicks; }
    public void changeInterval(long delta) { interval = Math.max(50, Math.min(60000, interval + delta)); updateFloatingLabel(); }
    public long getInterval() { return interval; }

    private void performClick(float cx, float cy) {
        Path path = new Path(); path.moveTo(cx, cy);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 35);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    public void showFloatingControls() {
        if (floating != null || wm == null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(10, 10, 10, 10);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.argb(235, 24, 28, 38)); bg.setCornerRadius(28); box.setBackground(bg);
        TextView title = new TextView(this); title.setText("AUTO"); title.setTextColor(Color.WHITE); title.setTextSize(12); title.setGravity(Gravity.CENTER); box.addView(title, new LinearLayout.LayoutParams(110, 32));
        TextView info = new TextView(this); info.setTag("info"); info.setTextColor(Color.LTGRAY); info.setGravity(Gravity.CENTER); info.setTextSize(11); box.addView(info, new LinearLayout.LayoutParams(110, 32));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER);
        Button slower = smallButton("−"); Button toggle = smallButton("▶"); Button faster = smallButton("+");
        row.addView(slower); row.addView(toggle); row.addView(faster); box.addView(row);
        slower.setOnClickListener(v -> changeInterval(100));
        faster.setOnClickListener(v -> changeInterval(-100));
        toggle.setOnClickListener(v -> { if (running) stopClicking(); else startClicking(); toggle.setText(running ? "Ⅱ" : "▶"); });
        box.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=floatingParams.x; oy=floatingParams.y; return true; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { floatingParams.x = ox + (int)(e.getRawX()-sx); floatingParams.y = oy + (int)(e.getRawY()-sy); wm.updateViewLayout(floating, floatingParams); return true; }
                return true;
            }
        });
        floating = box;
        floatingParams = new WindowManager.LayoutParams(130, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        floatingParams.gravity = Gravity.TOP | Gravity.END; floatingParams.x = 18; floatingParams.y = 180;
        wm.addView(floating, floatingParams); updateFloatingLabel();
    }

    private Button smallButton(String text) { Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(12); b.setMinWidth(0); b.setPadding(0,0,0,0); b.setBackgroundColor(Color.TRANSPARENT); b.setLayoutParams(new LinearLayout.LayoutParams(36, 42)); return b; }
    private void updateFloatingLabel() { if (floating == null) return; TextView info = floating.findViewWithTag("info"); if (info != null) info.setText((running ? "● اجرا" : "○ توقف") + "\n" + interval + "ms / " + performedClicks); }
    public void removeFloating() { if (floating != null && wm != null) { try { wm.removeView(floating); } catch (Exception ignored) {} floating=null; } }

    public void beginPick(int mode) {
        pickerMode = mode; wm = (WindowManager) getSystemService(WINDOW_SERVICE); if (picker != null) return;
        final SelectionView view = new SelectionView(); picker = view;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT);
        wm.addView(picker, p);
        Toast.makeText(this, mode == 1 ? "حالا نقطه موردنظر را لمس کن" : "انگشت را بکش و یک ناحیه انتخاب کن", Toast.LENGTH_SHORT).show();
    }

    private class SelectionView extends View {
        android.graphics.Paint paint = new android.graphics.Paint(1);
        SelectionView() { super(AutoClickService.this); paint.setColor(Color.argb(170,124,92,252)); paint.setStrokeWidth(4); setBackgroundColor(Color.argb(15,0,0,0)); }
        protected void onDraw(android.graphics.Canvas c) { super.onDraw(c); if (pickerMode == 2 && downX != 0) { float l=Math.min(downX,x), t=Math.min(downY,y), r=Math.max(downX,x), b=Math.max(downY,y); paint.setStyle(android.graphics.Paint.Style.FILL); paint.setColor(Color.argb(45,124,92,252)); c.drawRect(l,t,r,b,paint); paint.setStyle(android.graphics.Paint.Style.STROKE); paint.setColor(Color.argb(220,124,92,252)); c.drawRect(l,t,r,b,paint); } else if (pickerMode==1 && downX!=0) { paint.setStyle(android.graphics.Paint.Style.STROKE); paint.setColor(Color.argb(220,124,92,252)); c.drawCircle(downX,downY,22,paint); } }
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()==MotionEvent.ACTION_DOWN) { downX=e.getRawX(); downY=e.getRawY(); x=downX; y=downY; invalidate(); return true; }
            if (e.getAction()==MotionEvent.ACTION_MOVE) { x=e.getRawX(); y=e.getRawY(); invalidate(); return true; }
            if (e.getAction()==MotionEvent.ACTION_UP) { float ux=e.getRawX(), uy=e.getRawY(); if (pickerMode==1) { x=ux;y=uy;region=null; } else { region=new RectF(Math.min(downX,ux),Math.min(downY,uy),Math.max(downX,ux),Math.max(downY,uy)); x=region.centerX();y=region.centerY(); } removePicker(); return true; }
            return true;
        }
    }
    private void removePicker() { if (picker != null && wm != null) { try { wm.removeView(picker); } catch(Exception ignored){} picker=null; } }
}
