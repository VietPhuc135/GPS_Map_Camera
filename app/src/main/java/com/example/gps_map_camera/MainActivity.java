package com.example.gps_map_camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView cameraPreviewView;
    private ImageView imgCamera;
    private LinearLayout linearCard;
    private CardView cardView1;
    private CardView cardView2;
    private ImageCapture imageCapture;
    private static final String TAG = "MainActivity";

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

        cameraPreviewView = findViewById(R.id.camera);
        imgCamera = findViewById(R.id.img_camera);
        linearCard = findViewById(R.id.linear_card);
        cardView1 = findViewById(R.id.cardview1);
        cardView2 = findViewById(R.id.cardview2);

        startCamera();

        imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImageWithOverlay(MainActivity.this, cameraPreviewView, linearCard);
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera provider error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
    }

    private void captureImageWithOverlay(Context context, PreviewView previewView, View overlayView) {
        if (imageCapture == null) {
            return;
        }

        File photoFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera/IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(context), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri() != null ? outputFileResults.getSavedUri() : Uri.fromFile(photoFile);
                Bitmap capturedBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                Bitmap combinedBitmap = Bitmap.createBitmap(capturedBitmap.getWidth(), capturedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(combinedBitmap);
                canvas.drawBitmap(capturedBitmap, 0, 0, null);
                drawViewOnCanvas(overlayView, canvas);
                saveCombinedBitmap(combinedBitmap, photoFile);

                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(savedUri);
                context.sendBroadcast(mediaScanIntent);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Image capture failed: " + exception.getMessage());
                Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawViewOnCanvas(View view, Canvas canvas) {
        view.setDrawingCacheEnabled(true);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        view.setDrawingCacheEnabled(false);
    }

    private void saveCombinedBitmap(Bitmap combinedBitmap, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            Log.e(TAG, "Error saving combined bitmap: " + e.getMessage());
        }
    }
}