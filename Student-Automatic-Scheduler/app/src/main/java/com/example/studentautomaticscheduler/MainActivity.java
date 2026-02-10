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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;

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
                processText(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processText(String text) {
        DatabaseHelper db = new DatabaseHelper(this);
        db.getWritableDatabase().delete(DatabaseHelper.TABLE_SCHEDULE, null, null);

        String[] lines = text.split("\\r?\\n");
        String dayRegex = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)";
        String timeRegex = "\\d{2}:\\d{2}[AP]M\\s*-\\s*\\d{2}:\\d{2}[AP]M";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Detect Subject Code (e.g., CLDCOMP, INPROLA)
            if (line.matches("^[A-Z0-9]{4,7}$") || line.matches("^[A-Z]{4,7}\\d$")) {
                String subjectCode = line;
                StringBuilder subjectDesc = new StringBuilder();
                
                int j = i + 1;
                // Collect description until we find the Section (e.g., BSIT241A)
                while (j < lines.length && !lines[j].trim().matches("^[A-Z]{2,}[0-9]{3,}[A-Z]?.*")) {
                    subjectDesc.append(lines[j].trim()).append(" ");
                    j++;
                }
                
                if (j < lines.length) {
                    String sectionLine = lines[j].trim();
                    String section = sectionLine.split("\\s+")[0];
                    
                    // Extract days
                    List<String> days = new ArrayList<>();
                    Matcher dayMatcher = Pattern.compile(dayRegex).matcher(sectionLine);
                    if (dayMatcher.find()) days.add(dayMatcher.group());
                    
                    j++;
                    while (j < lines.length && Pattern.compile(dayRegex).matcher(lines[j]).find()) {
                        days.add(lines[j].trim());
                        j++;
                    }
                    
                    // Extract times
                    List<String> times = new ArrayList<>();
                    while (j < lines.length && Pattern.compile(timeRegex).matcher(lines[j]).find()) {
                        times.add(lines[j].trim());
                        j++;
                    }
                    
                    // Extract rooms - usually follows times
                    List<String> rooms = new ArrayList<>();
                    for (int k = 0; k < days.size(); k++) {
                        if (j < lines.length) {
                            rooms.add(lines[j].trim());
                            j++;
                        }
                    }
                    
                    // Extract Instructor - line usually ends with "Enrolled"
                    String instructor = "TBA";
                    while (j < lines.length) {
                        String potentialInstructor = lines[j].trim();
                        if (potentialInstructor.contains("Enrolled")) {
                            instructor = potentialInstructor.split("Enrolled")[0].trim();
                            break;
                        }
                        j++;
                    }
                    
                    // Insert each meeting into DB
                    for (int k = 0; k < days.size(); k++) {
                        String d = days.get(k);
                        String t = (k < times.size()) ? times.get(k) : (times.size() > 0 ? times.get(0) : "");
                        String r = (k < rooms.size()) ? rooms.get(k) : (rooms.size() > 0 ? rooms.get(0) : "");
                        db.insertSchedule(shortDay(d), t, subjectCode + " - " + subjectDesc.toString().trim(), section, r, instructor);
                    }
                    i = j; // Advance main loop
                }
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
            case "sunday": return "Sun";
            default: return day;
        }
    }
}
