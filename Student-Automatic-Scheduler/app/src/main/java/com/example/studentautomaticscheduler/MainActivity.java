package com.example.studentautomaticscheduler;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;
    private static final int SCAN_IMAGE = 101;
    private static final int PICK_IMAGE = 102;
    private static final int CAMERA_PERMISSION_CODE = 103;
    private static final int NOTIFICATION_PERMISSION_CODE = 104;
    private static final String TAG = "PDF_PARSER";
    private SQLiteDatabase db;

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

        findViewById(R.id.btnAddManual).setOnClickListener(v -> {
            ArrayList<ScheduleItem> emptyList = new ArrayList<>();
            emptyList.add(new ScheduleItem("", "", "", "", "", "", "", "", ""));
            Intent intent = new Intent(this, EditScheduleActivity.class);
            intent.putExtra("SCHEDULE_ITEMS", emptyList);
            intent.putExtra("SINGLE_EDIT_MODE", true);
            startActivity(intent);
        });

        // Initialize Database
        ReferenceDatabaseHelper refDb = new ReferenceDatabaseHelper(this);
        refDb.copyDatabase();
        db = openOrCreateDatabase("reference.db", MODE_PRIVATE, null);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    private String getSubjectDescriptionFromDb(String subjectCode) {
        if (db == null || subjectCode == null || subjectCode.isEmpty()) return null;
        try (Cursor cursor = db.rawQuery("SELECT subject_description FROM offerings_reference WHERE subject_code=? LIMIT 1", new String[]{subjectCode})) {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex("subject_description");
                if (idx != -1) return cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying DB for description", e);
        }
        return null;
    }

    private List<ScheduleItem> queryReference(String code, String section) {
        List<ScheduleItem> items = new ArrayList<>();
        if (db == null || code == null || section == null || code.isEmpty() || section.isEmpty()) return items;

        try (Cursor cursor = db.rawQuery("SELECT * FROM offerings_reference WHERE subject_code=? AND section=?", new String[]{code, section})) {
            int descIdx = cursor.getColumnIndex("subject_description");
            int dayIdx = cursor.getColumnIndex("day");
            int timeIdx = cursor.getColumnIndex("time");
            int roomIdx = cursor.getColumnIndex("room");
            int unitIdx = cursor.getColumnIndex("units");

            while (cursor.moveToNext()) {
                String d = (dayIdx != -1) ? cursor.getString(dayIdx) : "N/A";
                String t = (timeIdx != -1) ? cursor.getString(timeIdx) : "N/A";
                String desc = (descIdx != -1) ? cursor.getString(descIdx) : "";
                String r = (roomIdx != -1) ? cursor.getString(roomIdx) : "TBA";
                String u = (unitIdx != -1) ? cursor.getString(unitIdx) : "3.0";

                ScheduleItem item = new ScheduleItem(shortDay(d), t, desc, code, section, r, "", "Enrolled", u);
                item.classMode = getClassMode(r);
                items.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "DB Query Error", e);
        }
        return items;
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
        // String[] options = {"Choose PDF", "Scan from Camera", "Upload from Gallery"};
        String[] options = {"Choose PDF"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Update Schedule")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickFile("application/pdf", PICK_PDF);
                    } 
                    /* 
                    else if (which == 1) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            startActivityForResult(new Intent(this, CameraOcrActivity.class), SCAN_IMAGE);
                        } else {
                            androidx.core.app.ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        }
                    } else if (which == 2) {
                        pickFile("image/*", PICK_IMAGE);
                    }
                    */
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
            } else if (requestCode == SCAN_IMAGE) {
                String ocrText = data.getStringExtra("EXTRA_OCR_TEXT");
                if (ocrText != null) {
                    processText(ocrText);
                }
            } else if (requestCode == PICK_IMAGE) {
                processImageFromUri(data.getData());
            }
        }
    }

    private void processImageFromUri(Uri uri) {
        if (uri == null) return;
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            recognizer.process(image)
                .addOnSuccessListener(visionText -> processText(visionText.getText()))
                .addOnFailureListener(e -> Toast.makeText(this, "OCR Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "Error processing image uri", e);
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
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "No text detected.", Toast.LENGTH_SHORT).show();
            return;
        }

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
            String upper = line.toUpperCase();

            // Skip common headers and metadata
            if (line.isEmpty() || line.equalsIgnoreCase("TOTAL UNITS") || line.contains("STUDENT'S SCHEDULES")
                    || upper.contains("BACHELOR OF SCIENCE") || upper.contains("STUDENT NO")
                    || upper.contains("COLLEGE OF") || upper.contains("UNIVERSITY OF")
                    || upper.contains("ACADEMIC YEAR") || upper.contains("STUDENT NAME")
                    || upper.contains("STUDENT ID") || upper.contains("STUDENT COURSE")
                    || upper.contains("COMPUTER SCIENCE")) continue;

            // Skip table header lines (Subject Code Description Section etc.)
            int headerKeywords = 0;
            if (upper.contains("SUBJECT")) headerKeywords++;
            if (upper.contains("CODE")) headerKeywords++;
            if (upper.contains("DESCRIPTION")) headerKeywords++;
            if (upper.contains("SECTION")) headerKeywords++;
            if (upper.contains("INSTRUCTOR")) headerKeywords++;
            if (headerKeywords >= 2) continue;

            if (!subjectStarted && line.matches("^[A-Z0-9]{3,10}\\s+.*") && !line.contains("-") && !line.contains(":")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String candidateCode = parts[0];
                    if (candidateCode.equalsIgnoreCase("SUBJECT") || candidateCode.equalsIgnoreCase("SECTION")
                        || candidateCode.equalsIgnoreCase("STUDENT") || candidateCode.equalsIgnoreCase("DATE")
                        || candidateCode.equalsIgnoreCase("COURSE") || candidateCode.equalsIgnoreCase("NAME")
                        || candidateCode.equalsIgnoreCase("ISSUED") || candidateCode.equalsIgnoreCase("ROOM")) {
                        continue;
                    }

                    currentSubjCode = candidateCode;
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

                    String dbDesc = getSubjectDescriptionFromDb(currentSubjCode);
                    if (dbDesc != null) currentSubjDesc = dbDesc;
                }
            }
            
            if (subjectStarted) {
                if (currentSection == null || currentSection.isEmpty()) {
                    Matcher sm = Pattern.compile(sectionRegex).matcher(line);
                    if (sm.find()) {
                        currentSection = sm.group(1);
                        String before = line.substring(0, sm.start()).trim();
                        if (!before.isEmpty() && !Pattern.compile(dayRegex, Pattern.CASE_INSENSITIVE).matcher(before).find()) {
                            currentSubjDesc = (currentSubjDesc + " " + before).trim();
                        }
                    } else if (!line.matches(".*" + timeRegex + ".*") && !line.contains("Enrolled") && !Pattern.compile(dayRegex, Pattern.CASE_INSENSITIVE).matcher(line).find()) {
                        if (!line.matches("^[A-Z0-9]{3,8}\\s+.*") && !upper.contains("SUBJECT") && !upper.contains("SECTION")
                            && !upper.contains("ROOM") && !upper.contains("INSTRUCTOR") && !upper.contains("STATUS") && !upper.contains("UNITS")) {
                            currentSubjDesc = (currentSubjDesc + " " + line).trim();
                        }
                    }
                }

                Matcher dm = Pattern.compile(dayRegex).matcher(line);
                while (dm.find()) bDays.add(dm.group(1));

                Matcher tm = Pattern.compile(timeRegex).matcher(line);
                while (tm.find()) {
                    bTimes.add(tm.group(1));
                    String after = line.substring(tm.end()).trim();
                    if (after.length() > 2) {
                        String[] enrolledParts = after.split("Enrolled");
                        if (enrolledParts.length > 0) {
                            String roomCandidate = enrolledParts[0];
                            String[] unitParts = roomCandidate.split("\\d\\.\\d");
                            if (unitParts.length > 0) {
                                roomCandidate = unitParts[0].trim();
                            }
                            if (!roomCandidate.isEmpty() && !Pattern.compile(dayRegex).matcher(roomCandidate).find()
                                && !roomCandidate.equalsIgnoreCase("Room") && !roomCandidate.equalsIgnoreCase("Instructor")
                                && !roomCandidate.equalsIgnoreCase("Schedule") && !roomCandidate.equalsIgnoreCase("Status")
                                && !roomCandidate.equalsIgnoreCase("Units")) {
                                bRooms.add(roomCandidate);
                            }
                        }
                    }
                }

                if (bTimes.size() > bRooms.size()) {
                    if (!line.matches(".*" + timeRegex + ".*") && !Pattern.compile(dayRegex).matcher(line).find()
                        && !line.contains("Enrolled") && !line.matches("^[A-Z0-9]{3,8}\\s+.*") && !line.matches(sectionRegex)
                        && !line.equalsIgnoreCase("Room") && !line.equalsIgnoreCase("Instructor") && !line.equalsIgnoreCase("Units")
                        && !line.equalsIgnoreCase("Schedule") && !line.equalsIgnoreCase("Status")) {
                        bRooms.add(line);
                    }
                }
            }

            if (line.contains("Enrolled")) {
                String instructor = "";
                String[] enrolledParts = line.split("Enrolled");
                if (enrolledParts.length > 0) {
                    instructor = enrolledParts[0].trim();
                }

                String units = "3.0";
                Matcher um = Pattern.compile(unitsRegex).matcher(line);
                if (um.find()) units = um.group(1);

                List<ScheduleItem> dbItems = queryReference(currentSubjCode, currentSection);
                if (!dbItems.isEmpty()) {
                    for (ScheduleItem dbi : dbItems) {
                        if (dbi.instructor == null || dbi.instructor.isEmpty()) dbi.instructor = instructor;
                        if (!currentSubjDesc.isEmpty()) dbi.subject = currentSubjDesc;
                        
                        // Check for duplicates before adding
                        boolean exists = false;
                        for (ScheduleItem existing : parsedItems) {
                            if (existing.subjectCode.equalsIgnoreCase(dbi.subjectCode) &&
                                existing.day.equalsIgnoreCase(dbi.day) &&
                                existing.time.equalsIgnoreCase(dbi.time)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) parsedItems.add(dbi);
                    }
                } else {
                    int count = Math.max(bDays.size(), bTimes.size());
                    for (int k = 0; k < count; k++) {
                        String d = (k < bDays.size()) ? bDays.get(k) : (bDays.isEmpty() ? "N/A" : bDays.get(bDays.size()-1));
                        String t = (k < bTimes.size()) ? bTimes.get(k) : (bTimes.isEmpty() ? "N/A" : bTimes.get(bTimes.size()-1));
                        String r = (k < bRooms.size()) ? bRooms.get(k) : "TBA";

                        ScheduleItem newItem = new ScheduleItem(shortDay(d), t, currentSubjDesc, currentSubjCode, currentSection, r, instructor, "Enrolled", units);
                        newItem.classMode = getClassMode(r);

                        // Check for duplicates before adding
                        boolean exists = false;
                        for (ScheduleItem existing : parsedItems) {
                            if (existing.subjectCode.equalsIgnoreCase(newItem.subjectCode) &&
                                existing.day.equalsIgnoreCase(newItem.day) &&
                                existing.time.equalsIgnoreCase(newItem.time)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) parsedItems.add(newItem);
                    }
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

    private String getClassMode(String room) {
        if (room == null) return "Default";
        String r = room.toUpperCase();
        if (r.contains("V-") || r.contains("V") || r.contains("CCS")) {
            return "Online";
        } else if (r.contains("HSSH-") || r.contains("LAB") || r.contains("PE")) {
            return "Face-to-Face";
        }
        return "Default";
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
