package com.example.studentautomaticscheduler;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;
    private static final int SCAN_IMAGE = 101;
    private static final int PICK_IMAGE = 102;
    private static final int CAMERA_PERMISSION_CODE = 103;
    private static final int NOTIFICATION_PERMISSION_CODE = 104;
    private static final int EXPORT_PDF = 105;
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

        // Bottom Action Buttons
        findViewById(R.id.btnExport).setOnClickListener(v -> exportSchedule());
        findViewById(R.id.btnUpload).setOnClickListener(v -> showUploadOptions());
        findViewById(R.id.btnAdd).setOnClickListener(v -> openManualAdd());

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, Settings.class));
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
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String userApiKey = prefs.getString("user_gemini_api_key", "");

        List<String> optionsList = new ArrayList<>();
        optionsList.add("Choose PDF");

        // Only show OCR/Image options if the user has provided an AI key
        if (!userApiKey.isEmpty()) {
            optionsList.add("Scan from Camera");
            optionsList.add("Upload from Gallery");
        }

        String[] options = optionsList.toArray(new String[0]);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Update Schedule")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (selected.equals("Choose PDF")) {
                        pickFile("application/pdf", PICK_PDF);
                    } else if (selected.equals("Scan from Camera")) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            startActivityForResult(new Intent(this, CameraOcrActivity.class), SCAN_IMAGE);
                        } else {
                            androidx.core.app.ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        }
                    } else if (selected.equals("Upload from Gallery")) {
                        pickFile("image/*", PICK_IMAGE);
                    }
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
                    handleExtractedText(ocrText);
                }
            } else if (requestCode == PICK_IMAGE) {
                processImageFromUri(data.getData());
            } else if (requestCode == EXPORT_PDF) {
                performPdfExport(data.getData());
            }
        }
    }

    private void exportSchedule() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "My_Schedule.pdf");
        startActivityForResult(intent, EXPORT_PDF);
    }

    private void performPdfExport(Uri uri) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<ScheduleItem> allSchedules = dbHelper.getAllSchedules();
        if (PdfExportHelper.exportToPdf(this, uri, allSchedules)) {
            Toast.makeText(this, "Schedule exported successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to export schedule.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openManualAdd() {
        ArrayList<ScheduleItem> emptyList = new ArrayList<>();
        emptyList.add(new ScheduleItem("", "", "", "", "", "", "", "", ""));
        Intent intent = new Intent(this, EditScheduleActivity.class);
        intent.putExtra("SCHEDULE_ITEMS", emptyList);
        intent.putExtra("SINGLE_EDIT_MODE", true);
        startActivity(intent);
    }

    private void processImageFromUri(Uri uri) {
        if (uri == null) return;
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            recognizer.process(image)
                .addOnSuccessListener(visionText -> handleExtractedText(visionText.getText()))
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
                runOnUiThread(() -> handleExtractedText(text));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing PDF", e);
            }
        }).start();
    }

    private void processTextWithGemini(String rawExtractedText) {
        if (rawExtractedText == null || rawExtractedText.trim().isEmpty()) {
            Toast.makeText(this, "No text detected to send to Gemini.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String userApiKey = prefs.getString("user_gemini_api_key", "");

        if (userApiKey.isEmpty()) {
            Log.d("GEMINI_DEBUG", "No user API key found. Falling back to local parsing.");
            Toast.makeText(this, "No AI key provided. Using local parser.", Toast.LENGTH_SHORT).show();
            processText(rawExtractedText);
            return;
        }

        Log.d("GEMINI_DEBUG", "Starting Gemini parsing with user key...");

        // 1. Initialize the model using gemini-2.5-flash-lite
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash-lite",
                userApiKey
        );
        GenerativeModelFutures modelFutures = GenerativeModelFutures.from(gm);

        // 2. Formulate the precise prompt telling Gemini exactly how to map data structures and split up rows
        String structuredPrompt =
                "You are an expert data parsing assistant. Your task is to clean up a messy school schedule raw text output " +
                        "and convert it into a strictly formatted JSON array matching individual class items.\n\n" +
                        "CRITICAL RULES:\n" +
                        "1. If a single row contains multiple days (e.g. Day: 'Tuesday Friday', Schedule: '03:00PM-05:00PM'), " +
                        "you MUST split them up into separate distinct objects for each day.\n" +
                        "2. Keep day labels normalized to short text versions: 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'.\n" +
                        "3. Interpret abbreviations correctly: M=Mon, T=Tue, W=Wed, TH=Thu, F=Fri, S=Sat, TF=Tue & Fri, MTH=Mon & Thu, TTH=Tue & Thu.\n" +
                        "4. Match the room names relative to their line placement (e.g. if room states 'V-203 HSSH-203', map 'V-203' to the first day and 'HSSH-203' to the second day).\n" +
                        "5. If a field like 'instructor' is missing or labeled 'TBA', leave it as an empty string.\n" +
                        "6. Return ONLY a valid JSON code block array, no markdown wrappers outside of ```json.\n\n" +
                        "Expected JSON Output Structure:\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"day\": \"Tue\",\n" +
                        "    \"time\": \"03:00PM - 05:00PM\",\n" +
                        "    \"subject\": \"Business Process\",\n" +
                        "    \"subjectCode\": \"BUSPROS\",\n" +
                        "    \"section\": \"BSIT251B\",\n" +
                        "    \"room\": \"V-203\",\n" +
                        "    \"instructor\": \"Allainer C. Reyes\",\n" +
                        "    \"status\": \"Enrolled\",\n" +
                        "    \"units\": \"3.0\"\n" +
                        "  }\n" +
                        "]\n\n" +
                        "Here is the raw text content to extract:\n" + rawExtractedText;

        Content contentPrompt = new Content.Builder().addText(structuredPrompt).build();

        // 3. Execute Async call to the LLM backend
        ListenableFuture<GenerateContentResponse> responseFuture = modelFutures.generateContent(contentPrompt);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String rawJson = result.getText();
                if (rawJson != null) {
                    // Clean markdown artifacts if present
                    String cleanJson = rawJson.replace("```json", "").replace("```", "").trim();

                    runOnUiThread(() -> {
                        try {
                            // Parse JSON String directly into your existing Model array using Gson
                            Gson gson = new Gson();
                            Type listType = new TypeToken<ArrayList<ScheduleItem>>(){}.getType();
                            List<ScheduleItem> parsedItems = gson.fromJson(cleanJson, listType);

                            if (parsedItems != null && !parsedItems.isEmpty()) {
                                List<ScheduleItem> finalItems = new ArrayList<>();
                                List<String> processedKeys = new ArrayList<>();

                                for (ScheduleItem item : parsedItems) {
                                    // Handle duplicates if AI splits them but we want to look up in DB
                                    String key = item.subjectCode + "|" + item.section;
                                    if (processedKeys.contains(key)) continue;
                                    processedKeys.add(key);

                                    // Review Reference Database to enrich or correct AI data
                                    List<ScheduleItem> dbItems = queryReference(item.subjectCode, item.section);
                                    if (!dbItems.isEmpty()) {
                                        for (ScheduleItem dbi : dbItems) {
                                            // Prefer AI for instructor if DB is empty
                                            if (dbi.instructor == null || dbi.instructor.isEmpty()) dbi.instructor = item.instructor;
                                            // Prefer DB for other details, but keep AI description if DB is missing it
                                            if (dbi.subject == null || dbi.subject.isEmpty()) dbi.subject = item.subject;
                                            
                                            dbi.classMode = getClassMode(dbi.room);
                                            finalItems.add(dbi);
                                        }
                                    } else {
                                        // No DB match, use AI result directly
                                        item.classMode = getClassMode(item.room);
                                        finalItems.add(item);
                                    }
                                }
                                // Pass to verification screen
                                startEditActivity(finalItems);
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to build schedule from layout context.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("GEMINI_PARSE_ERROR", "JSON structural mismatch", e);
                            Toast.makeText(MainActivity.this, "Data formatting error occurred.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    Log.e("GEMINI_API_ERROR", "Call failed", t);
                    Toast.makeText(MainActivity.this, "API Connection Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleExtractedText(String text) {
        if (isNetworkAvailable()) {
            processTextWithGemini(text);
        } else {
            Toast.makeText(this, "Offline: Using local parser", Toast.LENGTH_SHORT).show();
            processText(text);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    private void processText(String text) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "No text detected.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScheduleItem> parsedItems = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        String dayRegex = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon|Tue|Wed|Thu|Fri|Sat|Sun|MTH|TF|WS|TTH|MWF|TH|\\b[MTWFS]\\b)";
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
                    } else if (!line.matches(".*" + timeRegex + ".*") && !line.contains("Enrolled") && !line.contains("Registered") && !Pattern.compile(dayRegex, Pattern.CASE_INSENSITIVE).matcher(line).find()) {
                        if (!line.matches("^[A-Z0-9]{3,8}\\s+.*") && !upper.contains("SUBJECT") && !upper.contains("SECTION")
                            && !upper.contains("ROOM") && !upper.contains("INSTRUCTOR") && !upper.contains("STATUS") && !upper.contains("UNITS")) {
                            currentSubjDesc = (currentSubjDesc + " " + line).trim();
                        }
                    }
                }

                Matcher dm = Pattern.compile(dayRegex).matcher(line);
                while (dm.find()) {
                    String matched = dm.group(1);
                    List<String> expanded = expandDayAbbreviation(matched);
                    if (!expanded.isEmpty()) bDays.addAll(expanded);
                    else bDays.add(matched);
                }

                Matcher tm = Pattern.compile(timeRegex).matcher(line);
                while (tm.find()) {
                    bTimes.add(tm.group(1));
                    String after = line.substring(tm.end()).trim();
                    if (after.length() > 2) {
                        String statusKeyword = after.contains("Enrolled") ? "Enrolled" : (after.contains("Registered") ? "Registered" : null);
                        if (statusKeyword != null) {
                            String[] statusParts = after.split(statusKeyword);
                            if (statusParts.length > 0) {
                                String roomCandidate = statusParts[0];
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
                }

                if (bTimes.size() > bRooms.size()) {
                    if (!line.matches(".*" + timeRegex + ".*") && !Pattern.compile(dayRegex).matcher(line).find()
                        && !line.contains("Enrolled") && !line.contains("Registered") && !line.matches("^[A-Z0-9]{3,8}\\s+.*") && !line.matches(sectionRegex)
                        && !line.equalsIgnoreCase("Room") && !line.equalsIgnoreCase("Instructor") && !line.equalsIgnoreCase("Units")
                        && !line.equalsIgnoreCase("Schedule") && !line.equalsIgnoreCase("Status")) {
                        bRooms.add(line);
                    }
                }
            }

            if (line.contains("Enrolled") || line.contains("Registered") || (subjectStarted && line.matches(".*\\d\\.\\d$"))) {
                String instructor = "";
                String status = line.contains("Registered") ? "Registered" : "Enrolled";
                String[] statusParts = line.split(status);
                if (statusParts.length > 0) {
                    instructor = statusParts[0].trim();
                }

                String units = "3.0";
                Matcher um = Pattern.compile(unitsRegex).matcher(line);
                if (um.find()) units = um.group(1);

                List<ScheduleItem> dbItems = queryReference(currentSubjCode, currentSection);
                if (!dbItems.isEmpty()) {
                    for (ScheduleItem dbi : dbItems) {
                        if (dbi.instructor == null || dbi.instructor.isEmpty()) dbi.instructor = instructor;
                        if (!currentSubjDesc.isEmpty()) dbi.subject = currentSubjDesc;
                        dbi.status = status;
                        
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

                        ScheduleItem newItem = new ScheduleItem(shortDay(d), t, currentSubjDesc, currentSubjCode, currentSection, r, instructor, status, units);
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

    private List<String> expandDayAbbreviation(String abbr) {
        List<String> days = new ArrayList<>();
        if (abbr == null) return days;
        String upper = abbr.toUpperCase().trim();
        switch (upper) {
            case "MTH": days.add("Mon"); days.add("Thu"); break;
            case "TF": days.add("Tue"); days.add("Fri"); break;
            case "WS": days.add("Wed"); days.add("Sat"); break;
            case "MWF": days.add("Mon"); days.add("Wed"); days.add("Fri"); break;
            case "TTH": days.add("Tue"); days.add("Thu"); break;
            case "M": days.add("Mon"); break;
            case "T": days.add("Tue"); break;
            case "W": days.add("Wed"); break;
            case "F": days.add("Fri"); break;
            case "S": days.add("Sat"); break;
            case "TH": days.add("Thu"); break;
        }
        return days;
    }


}
