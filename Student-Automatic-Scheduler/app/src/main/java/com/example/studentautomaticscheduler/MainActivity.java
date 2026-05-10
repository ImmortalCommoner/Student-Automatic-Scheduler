package com.example.studentautomaticscheduler;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;
    private static final int CAMERA_PERMISSION_CODE = 103;
    private static final int NOTIFICATION_PERMISSION_CODE = 104;
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

        checkNotificationPermission();

        String username = getIntent().getStringExtra("USERNAME");
        if (username != null) {
            TextView txtGreeting = findViewById(R.id.txtGreeting);
            txtGreeting.setText("Welcome, " + username);
        }

        findViewById(R.id.btnMonth).setOnClickListener(v -> loadFragment(new MonthFragment()));
        findViewById(R.id.btnWeek).setOnClickListener(v -> loadFragment(new WeekFragment()));
        findViewById(R.id.btnDay).setOnClickListener(v -> loadFragment(new DayFragment()));

        if (savedInstanceState == null) {
            loadDefaultFragment();
        }

        Button btnUpload = findViewById(R.id.btnUploadSchedule);
        btnUpload.setOnClickListener(v -> showUploadOptions());

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, Settings.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current != null) {
            loadFragment(current.getClass().getName().contains("Month") ? new MonthFragment() :
                         current.getClass().getName().contains("Day") ? new DayFragment() :
                         new WeekFragment());
        }
    }

    private void loadDefaultFragment() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int defaultView = prefs.getInt("default_view_pos", 1); 
        
        if (defaultView == 0) loadFragment(new MonthFragment());
        else if (defaultView == 2) loadFragment(new DayFragment());
        else loadFragment(new WeekFragment());
    }

    private void loadFragment(Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void showUploadOptions() {
        String[] options = {"Choose PDF"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Schedule")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) pickFile("application/pdf", PICK_PDF);
                })
                .show();
    }

    private void pickFile(String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        startActivityForResult(intent, requestCode);
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_PDF) {
                parsePDF(data.getData());
            }
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
                runOnUiThread(() -> processText(text));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing PDF", e);
            }
        }).start();
    }

    private void processText(String text) {
        List<ScheduleItem> parsedItems = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        String dayRegex = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon|Tue|Wed|Thu|Fri|Sat|Sun)";
        String timeRegex = "(\\d{1,2}:\\d{2}\\s*[AP]M\\s*-\\s*\\d{1,2}:\\d{2}\\s*[AP]M)";
        String sectionRegex = "([A-Z]{2,}\\d{2,}[A-Z]?)";
        String unitsRegex = "(\\d\\.\\d)";

        String currentSubjCode = "";
        String currentSubjDesc = "";
        String currentSection = "";
        
        List<String> bDays = new ArrayList<>();
        List<String> bTimes = new ArrayList<>();
        List<String> bRooms = new ArrayList<>();
        boolean subjectStarted = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.equalsIgnoreCase("TOTAL UNITS") || line.contains("STUDENT'S SCHEDULES")) continue;

            // 1. Detect Subject Header
            if (!subjectStarted && line.matches("^[A-Z0-9]{3,8}\\s+.*") && !line.contains("-") && !line.contains(":")) {
                String[] parts = line.split("\\s+");
                currentSubjCode = parts[0];
                int sectionIdx = -1;
                for (int j = 1; j < parts.length; j++) {
                    if (parts[j].matches(sectionRegex)) {
                        sectionIdx = j;
                        break;
                    }
                }
                if (sectionIdx != -1) {
                    currentSection = parts[sectionIdx];
                    StringBuilder desc = new StringBuilder();
                    for (int j = 1; j < sectionIdx; j++) desc.append(parts[j]).append(" ");
                    currentSubjDesc = desc.toString().trim();
                } else {
                    currentSubjDesc = line.substring(currentSubjCode.length()).trim();
                    currentSection = ""; 
                }
                subjectStarted = true;
                // Continue to extract days/times from header line
            } 
            
            if (subjectStarted) {
                // Section detection (if missing)
                if (currentSection.isEmpty()) {
                    Matcher sm = Pattern.compile(sectionRegex).matcher(line);
                    if (sm.find()) {
                        currentSection = sm.group(1);
                        String before = line.substring(0, sm.start()).trim();
                        if (!before.isEmpty() && !Pattern.compile(dayRegex, Pattern.CASE_INSENSITIVE).matcher(before).find()) {
                            currentSubjDesc = (currentSubjDesc + " " + before).trim();
                        }
                    } else if (!line.matches(".*" + timeRegex + ".*") && !line.contains("Enrolled") && !Pattern.compile(dayRegex, Pattern.CASE_INSENSITIVE).matcher(line).find()) {
                        if (!line.matches("^[A-Z0-9]{3,8}\\s+.*")) {
                            currentSubjDesc = (currentSubjDesc + " " + line).trim();
                        }
                    }
                }

                // Days
                Matcher dm = Pattern.compile(dayRegex).matcher(line);
                while (dm.find()) bDays.add(dm.group(1));

                // Times
                Matcher tm = Pattern.compile(timeRegex).matcher(line);
                while (tm.find()) {
                    bTimes.add(tm.group(1));
                    String after = line.substring(tm.end()).trim();
                    if (after.length() > 2) {
                        String roomCandidate = after.split("Enrolled")[0].split("\\d\\.\\d")[0].trim();
                        if (!roomCandidate.isEmpty() && !Pattern.compile(dayRegex).matcher(roomCandidate).find()) {
                            bRooms.add(roomCandidate);
                        }
                    }
                }

                // Room separate line
                if (bTimes.size() > bRooms.size()) {
                    if (!line.matches(".*" + timeRegex + ".*") && !Pattern.compile(dayRegex).matcher(line).find() 
                        && !line.contains("Enrolled") && !line.matches("^[A-Z0-9]{3,8}\\s+.*") && !line.matches(sectionRegex)) {
                        bRooms.add(line);
                    }
                }
            }

            // 3. Status/Instructor (Flush)
            if (line.contains("Enrolled")) {
                String instructor = line.split("Enrolled")[0].trim();
                String units = "3.0";
                Matcher um = Pattern.compile(unitsRegex).matcher(line);
                if (um.find()) units = um.group(1);

                int count = Math.max(bDays.size(), bTimes.size());
                for (int k = 0; k < count; k++) {
                    String d = (k < bDays.size()) ? bDays.get(k) : (bDays.isEmpty() ? "N/A" : bDays.get(bDays.size()-1));
                    String t = (k < bTimes.size()) ? bTimes.get(k) : (bTimes.isEmpty() ? "N/A" : bTimes.get(bTimes.size()-1));
                    String r = (k < bRooms.size()) ? bRooms.get(k) : "TBA";
                    
                    parsedItems.add(new ScheduleItem(shortDay(d), t, currentSubjDesc, currentSubjCode, currentSection, r, instructor, "Enrolled", units));
                }
                
                bDays.clear(); bTimes.clear(); bRooms.clear();
                subjectStarted = false;
                currentSubjCode = ""; currentSubjDesc = ""; currentSection = "";
            }
        }

        if (parsedItems.isEmpty()) {
            Toast.makeText(this, "No schedule detected.", Toast.LENGTH_LONG).show();
        } else {
            startEditActivity(parsedItems);
        }
    }

    private void startEditActivity(List<ScheduleItem> items) {
        Intent intent = new Intent(this, EditScheduleActivity.class);
        intent.putExtra("SCHEDULE_ITEMS", (Serializable) items);
        startActivity(intent);
    }

    private String shortDay(String day) {
        switch (day.toLowerCase()) {
            case "monday":
            case "mon": return "Mon";
            case "tuesday":
            case "tue": return "Tue";
            case "wednesday":
            case "wed": return "Wed";
            case "thursday":
            case "thu": return "Thu";
            case "friday":
            case "fri": return "Fri";
            case "saturday":
            case "sat": return "Sat";
            case "sunday":
            case "sun": return "Sun";
            default: return day;
        }
    }
}
