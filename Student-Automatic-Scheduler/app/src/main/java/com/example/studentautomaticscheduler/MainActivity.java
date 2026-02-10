package com.example.studentautomaticscheduler;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.net.Uri;
import android.util.Log;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;
    private static final String TAG = "PDF_PARSER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PDFBoxResourceLoader.init(getApplicationContext());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String username = getIntent().getStringExtra("USERNAME");
        if (username != null) {
            TextView txtGreeting = findViewById(R.id.txtGreeting);
            txtGreeting.setText("Welcome, " + username);
        }

        findViewById(R.id.btnMonth).setOnClickListener(v -> loadFragment(new MonthFragment()));
        findViewById(R.id.btnWeek).setOnClickListener(v -> loadFragment(new WeekFragment()));
        findViewById(R.id.btnDay).setOnClickListener(v -> loadFragment(new DayFragment()));

        if (savedInstanceState == null) {
            loadFragment(new WeekFragment());
        }

        Button btnUpload = findViewById(R.id.btnUploadSchedule);
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_PDF);
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, Settings.class));
        });
    }

    private void loadFragment(Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
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
                
                Log.d(TAG, "Extracted Text:\n" + text);
                processText(text);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing PDF", e);
            }
        }).start();
    }

    private void processText(String text) {
        DatabaseHelper db = new DatabaseHelper(this);
        db.getWritableDatabase().delete(DatabaseHelper.TABLE_SCHEDULE, null, null);

        String[] lines = text.split("\\r?\\n");
        String dayRegex = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)";
        String timeRegex = "(\\d{2}:\\d{2}[AP]M\\s*-\\s*\\d{2}:\\d{2}[AP]M)";
        // Matches things like BSIT241A or PHYSED02R
        String sectionRegex = "([A-Z]{2,}[0-9]{3,}[A-Z]?)";

        String currentSubject = "";
        String currentSection = "";
        List<String> currentDays = new ArrayList<>();
        List<String> currentTimes = new ArrayList<>();
        List<String> currentRooms = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("Student") || line.startsWith("TOTAL")) continue;

            // 1. Detect Subject Header (e.g., CLDCOMP CLOUD COMPUTING)
            // Typically starts with 4+ uppercase letters
            if (line.matches("^[A-Z]{4,}.*") && !line.contains(" - ")) {
                // If we were already tracking a subject, this is a new one, but wait... 
                // In this PDF, data for one subject can span many lines.
                // We'll reset only if we see a clear new subject code.
                if (line.split("\\s+")[0].matches("^[A-Z0-9]{4,8}$")) {
                    currentSubject = line;
                }
            }

            // 2. Detect Section
            Matcher sectionMatcher = Pattern.compile(sectionRegex).matcher(line);
            if (sectionMatcher.find()) {
                currentSection = sectionMatcher.group(1);
            }

            // 3. Detect Days
            Matcher dayMatcher = Pattern.compile(dayRegex).matcher(line);
            while (dayMatcher.find()) {
                currentDays.add(dayMatcher.group(1));
            }

            // 4. Detect Times
            Matcher timeMatcher = Pattern.compile(timeRegex).matcher(line);
            while (timeMatcher.find()) {
                currentTimes.add(timeMatcher.group(1));
            }

            // 5. Detect Room (Heuristic: If line contains Day/Time, the next part or next line is often Room)
            // This is tricky. Let's look for specific room patterns or strings like "ComLab" or "Room"
            if (line.contains("Lab") || line.contains("Room") || line.matches("^[A-Z]-\\d+$") || line.matches("^HSSH.*")) {
                currentRooms.add(line);
            }

            // 6. Detect Instructor & End of Block
            if (line.contains("Enrolled")) {
                String instructor = line.split("Enrolled")[0].trim();
                
                // We have reached the end of a subject block. Save all collected meetings.
                int count = Math.max(currentDays.size(), currentTimes.size());
                for (int m = 0; m < count; m++) {
                    String d = (m < currentDays.size()) ? currentDays.get(m) : "N/A";
                    String t = (m < currentTimes.size()) ? currentTimes.get(m) : "N/A";
                    String r = (m < currentRooms.size()) ? currentRooms.get(m) : "TBA";
                    
                    db.insertSchedule(shortDay(d), t, currentSubject, currentSection, r, instructor);
                }

                // Reset for next subject
                currentDays.clear();
                currentTimes.clear();
                currentRooms.clear();
                currentSection = "";
            }
        }
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Schedule Updated Successfully!", Toast.LENGTH_SHORT).show();
            recreate();
        });
    }

    private String shortDay(String day) {
        switch (day.toLowerCase()) {
            case "monday": return "Mon";
            case "tuesday": return "Tue";
            case "wednesday": return "Wed";
            case "thursday": return "Thu";
            case "friday": return "Fri";
            case "saturday": return "Sat";
            case "sunday": return "Sun";
            default: return day;
        }
    }
}
