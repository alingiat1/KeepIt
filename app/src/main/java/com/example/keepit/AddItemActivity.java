
package com.example.keepit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    private View backButton;
    private EditText etTitle, etPrice, etLink, etNote;
    private Spinner spinnerCategory;
    private CardView cardReminder, cardSelectImage;
    private TextView tvReminderTime;
    private Button btnSave;
    private ImageView imagePreview;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // שם קובץ התמונה שנשמר באחסון המקומי
    private String imageFileName = null;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        backButton = findViewById(R.id.backButton);
        etTitle = findViewById(R.id.etTitle);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);
        etNote = findViewById(R.id.etNote);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        cardReminder = findViewById(R.id.cardReminder);
        tvReminderTime = findViewById(R.id.tvReminderTime);
        cardSelectImage = findViewById(R.id.cardSelectImage);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        createImagePreview();

        String[] categories = {
                "מתכונים", "לקנות", "מתנות", "רעיונות", "מקומות"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );
        spinnerCategory.setAdapter(adapter);

        String incomingCategory =
                getIntent().getStringExtra("CATEGORY_NAME");

        if (incomingCategory != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(incomingCategory)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        backButton.setOnClickListener(v -> finish());

        // בחירת תמונה מהגלריה
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveGalleryImage(uri);
                    }
                }
        );

        // צילום תמונה במצלמה
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        saveCameraImage(bitmap);
                    }
                }
        );

        cardSelectImage.setOnClickListener(v -> {
            String[] options = {
                    "בחירה מהגלריה",
                    "צילום תמונה במצלמה",
                    "הסרת התמונה"
            };

            new AlertDialog.Builder(this)
                    .setTitle("הוספת תמונה")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            galleryLauncher.launch("image/*");
                        } else if (which == 1) {
                            cameraLauncher.launch(null);
                        } else {
                            removeSelectedImage();
                        }
                    })
                    .show();
        });

        cardReminder.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog =
                    new DatePickerDialog(
                            this,
                            (view, year, month, day) -> {
                                TimePickerDialog timePickerDialog =
                                        new TimePickerDialog(
                                                this,
                                                (view1, hour, minute) -> {
                                                    String formattedDate =
                                                            String.format(
                                                                    "%02d.%02d.%d",
                                                                    day,
                                                                    month + 1,
                                                                    year
                                                            );

                                                    String formattedTime =
                                                            String.format(
                                                                    "%02d:%02d",
                                                                    hour,
                                                                    minute
                                                            );

                                                    tvReminderTime.setText(
                                                            formattedDate
                                                                    + " - "
                                                                    + formattedTime
                                                    );
                                                },
                                                calendar.get(Calendar.HOUR_OF_DAY),
                                                calendar.get(Calendar.MINUTE),
                                                true
                                        );

                                timePickerDialog.show();
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );

            datePickerDialog.show();
        });

        btnSave.setOnClickListener(v -> saveItem());
    }

    // יצירת תצוגה מקדימה בתוך כרטיס בחירת התמונה
    private void createImagePreview() {
        if (!(cardSelectImage.getChildAt(0) instanceof ViewGroup)) {
            Toast.makeText(
                    this,
                    "לא ניתן להציג תצוגת תמונה. צריך לבדוק את קובץ ה-XML.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        ViewGroup content = (ViewGroup) cardSelectImage.getChildAt(0);

        imagePreview = new ImageView(this);
        imagePreview.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(180)
                )
        );

        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setVisibility(View.GONE);

        // התמונה תופיע בתחילת התוכן של הכרטיס
        content.addView(imagePreview, 0);
    }

    private void saveGalleryImage(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new Exception("לא ניתן לקרוא את התמונה");
            }

            File destination = createImageFile();
            copyStream(input, destination);

            imageFileName = destination.getName();
            showImagePreview(destination);

            Toast.makeText(
                    this,
                    "התמונה נשמרה במכשיר!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "לא ניתן לשמור את התמונה: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void saveCameraImage(Bitmap bitmap) {
        try {
            File destination = createImageFile();

            try (OutputStream output =
                         new FileOutputStream(destination)) {
                boolean success = bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        90,
                        output
                );

                if (!success) {
                    throw new Exception("שמירת התמונה נכשלה");
                }
            }

            imageFileName = destination.getName();
            showImagePreview(destination);

            Toast.makeText(
                    this,
                    "התמונה צולמה ונשמרה במכשיר!",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "לא ניתן לשמור את התמונה: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private File createImageFile() throws Exception {
        File directory = new File(getFilesDir(), "item_images");

        if (!directory.exists() && !directory.mkdirs()) {
            throw new Exception("לא ניתן ליצור תיקיית תמונות");
        }

        return File.createTempFile(
                "item_",
                ".jpg",
                directory
        );
    }

    private void copyStream(InputStream input, File destination)
            throws Exception {
        try (OutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int length;

            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }
    }

    private void showImagePreview(File file) {
        if (imagePreview == null) {
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        if (bitmap != null) {
            imagePreview.setImageBitmap(bitmap);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setContentDescription("תמונה שנבחרה לפריט");
        }
    }

    private void removeSelectedImage() {
        if (imageFileName != null) {
            File file = new File(
                    new File(getFilesDir(), "item_images"),
                    imageFileName
            );

            if (file.exists()) {
                file.delete();
            }
        }

        imageFileName = null;

        if (imagePreview != null) {
            imagePreview.setImageDrawable(null);
            imagePreview.setVisibility(View.GONE);
        }

        Toast.makeText(
                this,
                "התמונה הוסרה",
                Toast.LENGTH_SHORT
        ).show();
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }

    private void saveItem() {
        String title = etTitle.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String price = etPrice.getText().toString().trim();
        String link = etLink.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String reminder = tvReminderTime.getText().toString();

        if (title.isEmpty()) {
            etTitle.setError("נא להזין כותרת לפריט");
            etTitle.requestFocus();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "יש להתחבר לפני שמירת פריט",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnSave.setEnabled(false);

        Map<String, Object> itemData = new HashMap<>();

        itemData.put("userId", currentUser.getUid());
        itemData.put("title", title);
        itemData.put("category", category);
        itemData.put("price", price);
        itemData.put("link", link);
        itemData.put("note", note);
        itemData.put("reminder", reminder);

        // שומרים את שם הקובץ ב-Firestore, לא את נתיב הגלריה
        if (imageFileName != null) {
            itemData.put("imageFileName", imageFileName);
        }

        db.collection("items")
                .add(itemData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(
                            this,
                            "הפריט נשמר בהצלחה!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ITEM_TITLE", title);
                    resultIntent.putExtra(
                            "ITEM_ID",
                            documentReference.getId()
                    );

                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);

                    Toast.makeText(
                            this,
                            "שגיאה בשמירה: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
