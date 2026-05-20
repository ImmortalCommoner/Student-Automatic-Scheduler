package com.example.studentautomaticscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class CameraOcrActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private Button btnCapture;
    private ProgressBar progressBar;
    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;
    private ImageCapture imageCapture;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_ocr);

        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        progressBar = findViewById(R.id.progressBar);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        startCamera();

        btnCapture.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraOcr", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null || isProcessing) return;

        isProcessing = true;
        btnCapture.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        File photoFile = new File(getExternalFilesDir(null), "ocr_capture.jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                processImageFromFile(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraOcr", "Photo capture failed: " + exception.getMessage(), exception);
                isProcessing = false;
                runOnUiThread(() -> {
                    btnCapture.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CameraOcrActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void processImageFromFile(File file) {
        try {
            InputImage image = InputImage.fromFilePath(this, android.net.Uri.fromFile(file));
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String text = visionText.getText();
                        if (!text.isEmpty()) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("EXTRA_OCR_TEXT", text);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            resetUI("No text detected, try again");
                        }
                    })
                    .addOnFailureListener(e -> resetUI("OCR Error: " + e.getMessage()));
        } catch (java.io.IOException e) {
            Log.e("CameraOcr", "Error reading image file", e);
            resetUI("Error reading image");
        }
    }

    private void resetUI(String message) {
        isProcessing = false;
        runOnUiThread(() -> {
            btnCapture.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
