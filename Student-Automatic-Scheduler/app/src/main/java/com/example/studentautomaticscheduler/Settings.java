package com.example.studentautomaticscheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Settings extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        setupCustomizeView();
        setupNotificationView();
        setupAiConfig();
        setupHelpView();
        setupPersonalData();
    }

    private void setupAiConfig() {
        com.google.android.material.textfield.TextInputEditText etApiKey = findViewById(R.id.etGeminiApiKey);
        Button btnSaveKey = findViewById(R.id.btnSaveApiKey);

        String savedKey = prefs.getString("user_gemini_api_key", "");
        etApiKey.setText(savedKey);

        btnSaveKey.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            prefs.edit().putString("user_gemini_api_key", key).apply();
            Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCustomizeView() {
        // Dark Mode Setup
        SwitchCompat switchDarkMode = findViewById(R.id.switchDarkMode);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Default Calendar View Dropdown
        Spinner spinnerDefaultView = findViewById(R.id.spinnerDefaultView);
        String[] views = {"Month", "Week", "Day"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, views);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDefaultView.setAdapter(adapter);
        
        int savedViewPos = prefs.getInt("default_view_pos", 1); 
        spinnerDefaultView.setSelection(savedViewPos);
        spinnerDefaultView.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putInt("default_view_pos", position).apply();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupNotificationView() {
        // TTS Setup
        SwitchCompat switchTTS = findViewById(R.id.switchTTS);
        boolean isTTS = prefs.getBoolean("tts_enabled", false);
        switchTTS.setChecked(isTTS);
        switchTTS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("tts_enabled", isChecked).apply();
        });

        // Reminder Lead Time Dropdown
        Spinner spinnerNotificationTime = findViewById(R.id.spinnerNotificationTime);
        String[] timeOptions = {"On Time", "5 Minutes Before", "10 Minutes Before", "15 Minutes Before", "30 Minutes Before"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeOptions);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotificationTime.setAdapter(timeAdapter);

        int savedTimePos = prefs.getInt("notification_lead_time_pos", 3);
        spinnerNotificationTime.setSelection(savedTimePos);
        spinnerNotificationTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("notification_lead_time_pos", position).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupHelpView() {
        Button btnTutorialNUIS = findViewById(R.id.btnTutorialNUIS);
        btnTutorialNUIS.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("NUIS Export Instructions")
                    .setMessage("1. Log into your official NUIS Portal account.\n\n" +
                                "2. Navigate to your 'E-Enrollment' or 'Student Schedule' page.\n\n" +
                                "3. Click on the 'Print Schedule' or 'Download PDF' button.\n\n" +
                                "4. Open this Scheduler App, tap 'Update Schedule' on the Home view, and upload the saved document!")
                    .setPositiveButton("Got It", null)
                    .show();
        });
    }

    private void setupPersonalData() {
        Button btnClearData = findViewById(R.id.btnClearData);
        btnClearData.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Clear Data")
                    .setMessage("Are you sure you want to delete all schedule data?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        try {
                            DatabaseHelper db = new DatabaseHelper(this);
                            db.getWritableDatabase().delete("schedule", null, null);
                            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed clearing local records", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finishAffinity();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }
}
