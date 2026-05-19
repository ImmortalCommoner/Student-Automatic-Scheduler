package com.example.studentautomaticscheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
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
        setupPersonalData();
    }

    private void setupCustomizeView() {
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

        SwitchCompat switchTTS = findViewById(R.id.switchTTS);
        boolean isTTS = prefs.getBoolean("tts_enabled", false);
        switchTTS.setChecked(isTTS);
        switchTTS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("tts_enabled", isChecked).apply();
        });

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

//        SeekBar seekBarFontSize = findViewById(R.id.seekBarFontSize);
//        int savedFontSize = prefs.getInt("font_size", 5);
//        seekBarFontSize.setProgress(savedFontSize);
//        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                prefs.edit().putInt("font_size", progress).apply();
//            }
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });
    }

    private void setupPersonalData() {
        Button btnClearData = findViewById(R.id.btnClearData);
        btnClearData.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Clear Data")
                    .setMessage("Are you sure you want to delete all schedule data?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper db = new DatabaseHelper(this);
                        db.getWritableDatabase().delete("schedule", null, null);
                        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
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
