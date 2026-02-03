package com.example.studentautomaticscheduler;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.widget.*;
import android.view.View;
import android.net.Uri;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnMonth = findViewById(R.id.btnMonth);
        Button btnWeek = findViewById(R.id.btnWeek);
        Button btnDay = findViewById(R.id.btnDay);

        btnMonth.setOnClickListener(v -> loadFragment(new MonthFragment()));
        btnWeek.setOnClickListener(v -> loadFragment(new WeekFragment()));
        btnDay.setOnClickListener(v -> loadFragment(new DayFragment()));

        if (savedInstanceState == null) {
            loadFragment(new WeekFragment()); // default view
        }

        Button btnUpload = findViewById(R.id.btnUploadSchedule);

        btnUpload.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_PDF);
        });

    }

    private void loadFragment(Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, fragment)
                .commit();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF && resultCode == RESULT_OK) {

            Uri uri = data.getData();

            parsePDF(uri);
        }
    }

    private void parsePDF(Uri uri) {


        new Thread(() -> {

            try {

                InputStream inputStream = getContentResolver().openInputStream(uri);

                com.tom_roush.pdfbox.pdmodel.PDDocument document =
                        com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream);

                com.tom_roush.pdfbox.text.PDFTextStripper stripper =
                        new com.tom_roush.pdfbox.text.PDFTextStripper();

                String text = stripper.getText(document);

                document.close();

                processText(text);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void processText(String text) {

        DatabaseHelper db = new DatabaseHelper(this);

        db.getWritableDatabase().delete("schedule", null, null);

        String[] lines = text.split("\n");

        String currentSubject = "";

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            // skip empty
            if (line.isEmpty()) continue;

            // detect subject line (subject code + description)
            // example: INPROLA INTRODUCTION TO PROGRAMMING
            if (line.matches("^[A-Z]{4,}.*")) {
                currentSubject = cleanSubject(line);

                continue;
            }

            // detect day pair (Monday Tuesday etc.)
            if (line.matches("(?i).*(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday).*")) {

                String day1 = line.split(" ")[line.split(" ").length - 1];

                // next line usually second day
                String day2 = lines[++i].trim();

                // next two lines are times
                String time1 = lines[++i].trim();
                String time2 = lines[++i].trim();

                // insert BOTH schedule entries
                db.insert(shortDay(day1), time1, currentSubject);
                db.insert(shortDay(day2), time2, currentSubject);
            }
        }

        runOnUiThread(this::recreate);
    }

    private String shortDay(String day) {

        switch (day.toLowerCase()) {
            case "monday": return "Mon";
            case "tuesday": return "Tue";
            case "wednesday": return "Wed";
            case "thursday": return "Thu";
            case "friday": return "Fri";
            case "saturday": return "Sat";
            default: return day;
        }
    }

    private String cleanSubject(String raw) {

        // remove first code word (INPROLA, CLDCOMP, etc.)
        String[] parts = raw.split(" ", 2);

        if (parts.length > 1)
            return parts[1];

        return raw;
    }





}

