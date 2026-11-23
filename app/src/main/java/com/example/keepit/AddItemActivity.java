
package com.example.keepit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.firestore.FieldValue;
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

    private String imageFileName = null;
    private String originalImageFileName = null;
    private String itemId = null;

    private boolean isEditMode = false;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    private final String[] categories = {
            "מתכונים", "לקנות", "מתנות", "רעיונות", "מקומות"
    };

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

        createImagePreview();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        );

        spinnerCategory.setAdapter(adapter);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveGalleryImage(uri);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        saveCameraImage(bitmap);
                    }
                }
        );

        backButton.setOnClickListener(v -> finish());

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

        cardReminder.setOnClickListener(v -> chooseReminder());

        btnSave.setOnClickListener(v -> saveItem());

        itemId = getIntent().getStringExtra("ITEM_ID");
        isEditMode = itemId != null && !itemId.isEmpty();

        if (isEditMode) {
            btnSave.setText("שמירת שינויים");
            loadItemForEditing();

        } else {
            String incomingCategory =
                    getIntent().getStringExtra("CATEGORY_NAME");

            if (incomingCategory != null) {
                selectCategory(incomingCategory);
            }
        }
    }

    private void loadItemForEditing() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        btnSave.setEnabled(false);

        db.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(
                                this,
                                "הפריט לא נמצא",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                        return;
                    }

                    String ownerId = document.getString("userId");

                    if (ownerId == null ||
                            !ownerId.equals(user.getUid())) {

                        Toast.makeText(
                                this,
                                "אין הרשאה לערוך את הפריט",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                        return;
                    }

                    etTitle.setText(document.getString("title"));
                    etPrice.setText(document.getString("price"));
                    etLink.setText(document.getString("link"));
                    etNote.setText(document.getString("note"));

                    String category = document.getString("category");

                    if (category != null) {
                        selectCategory(category);
                    }

                    String reminder = document.getString("reminder");

                    if (reminder != null) {
                        tvReminderTime.setText(reminder);
                    }

                    imageFileName =
                            document.getString("imageFileName");

                    originalImageFileName = imageFileName;

                    if (imageFileName != null) {

                        File imageFile = getImageFile(imageFileName);

                        if (imageFile.exists()) {
                            showImagePreview(imageFile);
                        }
                    }

                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {

                    btnSave.setEnabled(true);

                    Toast.makeText(
                            this,
                            "שגיאה בטעינת הפריט: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void selectCategory(String category) {

        for (int i = 0; i < categories.length; i++) {

            if (categories[i].equals(category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void chooseReminder() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {

                            TimePickerDialog timePickerDialog =
                                    new TimePickerDialog(
                                            this,
                                            (view1, hour, minute) -> {

                                                String date = String.format(
                                                        "%02d.%02d.%d",
                                                        day,
                                                        month + 1,
                                                        year
                                                );

                                                String time = String.format(
                                                        "%02d:%02d",
                                                        hour,
                                                        minute
                                                );

                                                tvReminderTime.setText(
                                                        date + " - " + time
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
    }

    private void createImagePreview() {

        if (!(cardSelectImage.getChildAt(0) instanceof ViewGroup)) {

            Toast.makeText(
                    this,
                    "לא ניתן להציג תצוגת תמונה. צריך לבדוק את קובץ ה-XML.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ViewGroup content =
                (ViewGroup) cardSelectImage.getChildAt(0);

        imagePreview = new ImageView(this);

        imagePreview.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(180)
                )
        );

        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setVisibility(View.GONE);

        content.addView(imagePreview, 0);
    }

    private void saveGalleryImage(Uri uri) {

        try (InputStream input =
                     getContentResolver().openInputStream(uri)) {

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

        File directory = new File(
                getFilesDir(),
                "item_images"
        );

        if (!directory.exists() && !directory.mkdirs()) {
            throw new Exception("לא ניתן ליצור תיקיית תמונות");
        }

        return File.createTempFile(
                "item_",
                ".jpg",
                directory
        );
    }

    private File getImageFile(String fileName) {

        return new File(
                new File(getFilesDir(), "item_images"),
                fileName
        );
    }

    private void copyStream(
            InputStream input,
            File destination
    ) throws Exception {

        try (OutputStream output =
                     new FileOutputStream(destination)) {

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

        Bitmap bitmap = BitmapFactory.decodeFile(
                file.getAbsolutePath()
        );

        if (bitmap != null) {
            imagePreview.setImageBitmap(bitmap);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setContentDescription(
                    "תמונה שנבחרה לפריט"
            );
        }
    }

    private void removeSelectedImage() {

        imageFileName = null;

        if (imagePreview != null) {
            imagePreview.setImageDrawable(null);
            imagePreview.setVisibility(View.GONE);
        }

        Toast.makeText(
                this,
                "התמונה תוסר לאחר שמירת השינויים",
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

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "יש להתחבר לפני שמירת פריט",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnSave.setEnabled(false);

        Map<String, Object> itemData = new HashMap<>();

        itemData.put("userId", user.getUid());
        itemData.put("title", title);
        itemData.put("category", category);
        itemData.put("price", price);
        itemData.put("link", link);
        itemData.put("note", note);
        itemData.put("reminder", reminder);

        if (imageFileName != null) {
            itemData.put("imageFileName", imageFileName);
        } else {
            itemData.put("imageFileName", FieldValue.delete());
        }

        if (isEditMode) {
            updateExistingItem(itemData);
        } else {
            createNewItem(itemData);
        }
    }

    private void createNewItem(Map<String, Object> itemData) {

        db.collection("items")
                .add(itemData)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            this,
                            "הפריט נשמר בהצלחה!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent result = new Intent();
                    result.putExtra(
                            "ITEM_ID",
                            documentReference.getId()
                    );

                    setResult(RESULT_OK, result);
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

    private void updateExistingItem(Map<String, Object> itemData) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            btnSave.setEnabled(true);
            return;
        }

        db.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(document -> {

                    String ownerId = document.getString("userId");

                    if (!document.exists() ||
                            ownerId == null ||
                            !ownerId.equals(user.getUid())) {

                        btnSave.setEnabled(true);

                        Toast.makeText(
                                this,
                                "לא ניתן לעדכן את הפריט",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    db.collection("items")
                            .document(itemId)
                            .update(itemData)
                            .addOnSuccessListener(unused -> {

                                if (originalImageFileName != null &&
                                        !originalImageFileName.equals(imageFileName)) {

                                    File oldImage =
                                            getImageFile(originalImageFileName);

                                    if (oldImage.exists()) {
                                        oldImage.delete();
                                    }
                                }

                                Toast.makeText(
                                        this,
                                        "השינויים נשמרו בהצלחה!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                Intent result = new Intent();
                                result.putExtra("ITEM_ID", itemId);

                                setResult(RESULT_OK, result);
                                finish();
                            })
                            .addOnFailureListener(e -> {

                                btnSave.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "שגיאה בעדכון: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    btnSave.setEnabled(true);

                    Toast.makeText(
                            this,
                            "שגיאה בבדיקת הפריט: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
