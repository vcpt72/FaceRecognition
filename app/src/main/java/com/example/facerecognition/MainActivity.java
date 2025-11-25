package com.example.facerecognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btnDetectFaces;
    private LinearLayout smileResultContainer;
    private List<Face> detectedFaces;
    private Bitmap originalBitmapCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI Views
        imageView = findViewById(R.id.imageView);
        btnDetectFaces = findViewById(R.id.btnDetectFaces);
        Button btnSmile = findViewById(R.id.btnDetectSmile);
        Button btnBlurFaces = findViewById(R.id.btnBlurFaces);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnSavePhoto = findViewById(R.id.btnSavePhoto);
        smileResultContainer = findViewById(R.id.smileResultContainer);


        Bitmap testBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.face_test);
        imageView.setImageBitmap(testBitmap);
        originalBitmapCopy = testBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Button listeners
        btnDetectFaces.setOnClickListener(v -> detectFaces());
        btnBlurFaces.setOnClickListener(v->blurDetectedFaces());
        btnSmile.setOnClickListener(v -> showSmileResults());
        btnSavePhoto.setOnClickListener(v->saveImage());
        btnReset.setOnClickListener(v-> resetImage());
    }

    private void detectFaces() {
        if (imageView.getDrawable() == null) return;

        Bitmap originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        InputImage image = InputImage.fromBitmap(originalBitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)  // Needed for smile
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    detectedFaces = faces;
                    drawBoundingBoxes(faces, originalBitmap);
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void drawBoundingBoxes(List<Face> faces, Bitmap originalBitmap) {
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        float scaleFactor = originalBitmap.getWidth() / 1080f;
        float stroke = 6 * scaleFactor;
        float textSize = 40 * scaleFactor;

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);

        int index = 1;

        for (Face face : faces) {
            Rect box = face.getBoundingBox();
            canvas.drawRect(box, paint);


            canvas.drawText("Face " + index, box.left, box.top - 10, textPaint);

            index++;
        }

        imageView.setImageBitmap(mutableBitmap);
    }

    private void showSmileResults() {
        if (detectedFaces == null || detectedFaces.isEmpty()) {
            return;
        }

        smileResultContainer.removeAllViews();
        int index = 1;

        for (Face face : detectedFaces) {
            Float probability = face.getSmilingProbability();
            String text;

            if (probability != null && probability >= 0) {
                text = String.format("Face %d smile: %.2f", index, probability);
            } else {
                text = "Face " + index + " smile: Neznámé";
            }

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(16f);
            tv.setPadding(4, 4, 4, 4);
            smileResultContainer.addView(tv);

            index++;
        }
    }
    private void blurDetectedFaces() {
        if (detectedFaces == null || detectedFaces.isEmpty() || imageView.getDrawable() == null) {
            return;
        }

        Bitmap originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        for (Face face : detectedFaces) {
            Rect box = face.getBoundingBox();
            canvas.drawRect(box, paint);
        }

        imageView.setImageBitmap(mutableBitmap);
    }
    private void saveImage() {

        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        if (bitmap == null) return;

        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "face_result_" + System.currentTimeMillis(),
                "Obrázek s rozpoznanými obličeji"
        );

        if (savedImageURL != null) {
            Toast.makeText(this, "Uloženo do galerie ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Chyba ukládání ", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetImage() {
        if (originalBitmapCopy != null) {
            imageView.setImageBitmap(originalBitmapCopy.copy(Bitmap.Config.ARGB_8888, true));
        }
        if (detectedFaces != null) {
            detectedFaces.clear();
        }
        smileResultContainer.removeAllViews();
    }

}
