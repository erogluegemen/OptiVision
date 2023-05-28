package com.example.facedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // look at this one
import android.graphics.Rect;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.Toast;

import com.camerakit.CameraKitView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.mlkit.vision.common.InputImage;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import com.example.facedetection.Helper.GraphicOverlay;
import com.example.facedetection.Helper.RectOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CameraKitView cameraKitView;
    private Button faceDetectButton;
    private AlertDialog alertDialog;
    private GraphicOverlay graphicOverlay;
    private FaceDetector faceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraKitView = findViewById(R.id.camera);

        faceDetectButton = findViewById(R.id.detect);
        faceDetectButton.setOnClickListener(this);

        graphicOverlay = findViewById(R.id.graphic_overlay);

        // Set required camera permissions
        cameraKitView.setPermissions(CameraKitView.PERMISSION_CAMERA);

        // Set camera listener for open and close events
        cameraKitView.setCameraListener(new CameraKitView.CameraListener() {
            @Override
            public void onOpened() {
                // Camera opened
            }

            @Override
            public void onClosed() {
                // Camera closed
            }
        });

        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        faceDetector = FaceDetection.getClient(highAccuracyOpts);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.detect) {
            showAlertDialogWithAutoDismiss();
            captureImage();
            graphicOverlay.clear();
        }
    }

    private void captureImage() {
        cameraKitView.captureImage(new CameraKitView.ImageCallback() {
            @Override
            public void onImage(CameraKitView cameraKitView, final byte[] capturedImage) {
                // Process the captured image
                showAlertDialogWithAutoDismiss();

                // Convert the captured image byte array to a Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage.length);

                // Get the original dimensions of the captured image
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage.length, options);
                int imageWidth = options.outWidth;
                int imageHeight = options.outHeight;

                // Resize the bitmap using the original dimensions of the captured image
                bitmap = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, false);

                cameraKitView.stopVideo();
                detectFace(bitmap);
            }
        });
    }


    private void detectFace(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        drawFace(faces);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Error detecting faces", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    }
                });
    }

    private void drawFace(List<Face> faces) {
        for (Face face: faces){
            Rect rect = face.getBoundingBox();
            RectOverlay rectOverlay = new RectOverlay(graphicOverlay, rect);
            graphicOverlay.add(rectOverlay);
        }

        // Dismiss the dialog after drawing the faces
        alertDialog.dismiss();
    }


    public void  showAlertDialogWithAutoDismiss(){
        alertDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Lütfen bekleyiniz...")
                .setCancelable(false)
                .build();

        alertDialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (alertDialog.isShowing()){
                    alertDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Yüz Tespit Edilemedi", Toast.LENGTH_LONG).show();
                    cameraKitView.startVideo();
                }
            }
        }, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceDetector != null) {
            faceDetector.close();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
        cameraKitView.startVideo();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
        cameraKitView.stopVideo();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
        cameraKitView.stopVideo();
    }
}

