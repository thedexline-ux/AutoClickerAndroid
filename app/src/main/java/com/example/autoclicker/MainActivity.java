package com.example.autoclicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText xInput, yInput, intervalInput, countInput;
    private TextView statusText;
    private int selectedProfile = 0;

    private final float[] xs = new float[4];
    private final float[] ys = new float[4];
    private final long[] intervals = new long[4];
    private final long[] counts = new long[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xInput = findViewById(R.id.xInput);
        yInput = findViewById(R.id.yInput);
        intervalInput = findViewById(R.id.intervalInput);
        countInput = findViewById(R.id.clickCountInput);
        statusText = findViewById(R.id.statusText);

        for (int i = 0; i < 4; i++) {
            xs[i] = 500;
            ys[i] = 500;
            intervals[i] = 1000;
            counts[i] = 0;
        }
        showProfile();

        findViewById(R.id.accessibilityButton).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            saveProfile();
            Toast.makeText(this, "پروفایل ذخیره شد", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.startButton).setOnClickListener(v -> {
            saveProfile();
            AutoClickService service = AutoClickService.getInstance();
            if (service == null) {
                Toast.makeText(this, "ابتدا سرویس دسترسی را فعال کن", Toast.LENGTH_LONG).show();
                return;
            }
            service.setClickPosition(xs[selectedProfile], ys[selectedProfile]);
            service.setInterval(intervals[selectedProfile]);
            service.setMaxClicks(counts[selectedProfile]);
            service.startClicking();
            statusText.setText("وضعیت: در حال کلیک");
        });

        findViewById(R.id.stopButton).setOnClickListener(v -> {
            AutoClickService service = AutoClickService.getInstance();
            if (service != null) service.stopClicking();
            statusText.setText("وضعیت: متوقف");
        });

        findViewById(R.id.profile1).setOnClickListener(v -> selectProfile(0));
        findViewById(R.id.profile2).setOnClickListener(v -> selectProfile(1));
        findViewById(R.id.profile3).setOnClickListener(v -> selectProfile(2));
        findViewById(R.id.profile4).setOnClickListener(v -> selectProfile(3));
    }

    private void selectProfile(int p) {
        saveProfile();
        selectedProfile = p;
        showProfile();
    }

    private void showProfile() {
        xInput.setText(String.valueOf((int) xs[selectedProfile]));
        yInput.setText(String.valueOf((int) ys[selectedProfile]));
        intervalInput.setText(String.valueOf(intervals[selectedProfile]));
        countInput.setText(String.valueOf(counts[selectedProfile]));
    }

    private void saveProfile() {
        try {
            xs[selectedProfile] = Float.parseFloat(xInput.getText().toString());
            ys[selectedProfile] = Float.parseFloat(yInput.getText().toString());
            intervals[selectedProfile] = Math.max(50,
                    Long.parseLong(intervalInput.getText().toString()));
            counts[selectedProfile] = Math.max(0,
                    Long.parseLong(countInput.getText().toString()));
        } catch (Exception e) {
            Toast.makeText(this, "مقادیر واردشده صحیح نیست", Toast.LENGTH_SHORT).show();
        }
    }
}
