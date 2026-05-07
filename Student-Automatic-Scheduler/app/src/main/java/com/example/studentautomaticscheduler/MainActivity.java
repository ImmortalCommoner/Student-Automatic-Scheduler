package com.example.studentautomaticscheduler;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.net.Uri;
import android.util.Log;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF = 100;
    private static final int PICK_IMAGE = 101;
    private static final int CAPTURE_OCR = 102;
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
        btnUpload.setOnClickListener(v -> {
            showUploadOptions();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, Settings.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the current fragment to reflect any data changes or settings
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current != null) {
            loadFragment(current.getClass().getName().contains("Month") ? new MonthFragment() :
                         current.getClass().getName().contains("Day") ? new DayFragment() :
                         new WeekFragment());
        }
    }

    private void loadDefaultFragment() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int defaultView = prefs.getInt("default_view_pos", 1); // 0: Month, 1: Week, 2: Day
        
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
        String[] options = {"Upload PDF", "Upload Image", "Take Photo"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Schedule Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("application/pdf");
                        startActivityForResult(intent, PICK_PDF);
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, PICK_IMAGE);
                    } else if (which == 2) {
                        checkCameraPermission();
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCameraActivity();
        }
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

    private void startCameraActivity() {
        Intent intent = new Intent(this, CameraOcrActivity.class);
        startActivityForResult(intent, CAPTURE_OCR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCameraActivity();
            } else {
                Toast.makeText(this, "Camera permission is required to scan schedule", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_PDF) {
                Uri uri = data.getData();
                parsePDF(uri);
            } else if (requestCode == PICK_IMAGE) {
                Uri uri = data.getData();
                recognizeTextFromImage(uri);
            } else if (requestCode == CAPTURE_OCR) {
                String resultText = data.getStringExtra("EXTRA_OCR_TEXT");
                if (resultText != null) {
                    processText(resultText);
                }
            }
        }
    }

    private void recognizeTextFromImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String resultText = visionText.getText();
                        Log.d(TAG, "OCR Extracted Text:\n" + resultText);
                        processText(resultText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR failed", e);
                        Toast.makeText(this, "Text recognition failed", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading image for OCR", e);
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
        String dayRegex = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon|Tue|Wed|Thu|Fri|Sat|Sun)";
        String timeRegex = "(\\d{1,2}:\\d{2}\\s*[AP]M\\s*-\\s*\\d{1,2}:\\d{2}\\s*[AP]M)";
        String sectionRegex = "([A-Z]{2,}[0-9]{2,}[A-Z]?)";

        String currentSubject = "";
        String currentSection = "";
        List<String> currentDays = new ArrayList<>();
        List<String> currentTimes = new ArrayList<>();
        List<String> currentRooms = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.equalsIgnoreCase("Subject") ||
                    line.equalsIgnoreCase("Code") ||
                    line.contains("Subject Description") ||
                    line.contains("Section Day") ||
                    line.contains("Schedule Room") ||
                    line.contains("Instructor Status") ||
                    line.contains("Units")) {
                continue;
            }

            if (line.matches("^[A-Z]{4,}.*") && !line.contains(" - ")) {
                if (line.split("\\s+")[0].matches("^[A-Z0-9]{4,8}$")) {
                    currentSubject = line;
                }
            }

            Matcher sectionMatcher = Pattern.compile(sectionRegex).matcher(line);
            if (sectionMatcher.find()) {
                currentSection = sectionMatcher.group(1);
            }

            Matcher dayMatcher = Pattern.compile(dayRegex).matcher(line);
            while (dayMatcher.find()) {
                currentDays.add(dayMatcher.group(1));
            }

            Matcher timeMatcher = Pattern.compile(timeRegex).matcher(line);
            while (timeMatcher.find()) {
                currentTimes.add(timeMatcher.group(1));
            }

            if (line.matches(".*Lab.*") ||
                    line.matches(".*ComLab.*") ||
                    line.matches("PE Room \\d+") ||
                    line.matches("HSSH-\\d+") ||
                    line.matches("V-\\d+")) {
                currentRooms.add(line);
            }


            if (line.matches(".*Enrolled.*\\d+\\.\\d+")) {
                String instructor = line.split("Enrolled")[0].trim();
                
                int count = Math.max(currentDays.size(), currentTimes.size());
                for (int m = 0; m < count; m++) {
                    String d = (m < currentDays.size()) ? currentDays.get(m) : "N/A";
                    String t = (m < currentTimes.size()) ? currentTimes.get(m) : "N/A";
                    String r = (m < currentRooms.size()) ? currentRooms.get(m) : "TBA";
                    
                    db.insertSchedule(shortDay(d), t, currentSubject, currentSection, r, instructor);
                }

                currentDays.clear();
                currentTimes.clear();
                currentRooms.clear();
                currentSection = "";
            }
        }
        
        // After processing, schedule notifications
        NotificationHelper.scheduleClassReminders(this);
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Schedule Updated & Notifications Set!", Toast.LENGTH_SHORT).show();
            recreate();
        });
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
