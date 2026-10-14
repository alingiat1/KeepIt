package com.example.keepit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private FirebaseFirestore db;
    private Uri imageUri = null;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        db = FirebaseFirestore.getInstance();

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

        String[] categories = {"מתכונים", "לקנות", "מתנות", "רעיונות", "מקומות"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        String incomingCategory = getIntent().getStringExtra("CATEGORY_NAME");
        if (incomingCategory != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(incomingCategory)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        backButton.setOnClickListener(v -> finish());

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        Toast.makeText(this, "התמונה נבחרה בהצלחה!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "התצלום צולם בהצלחה!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cardSelectImage.setOnClickListener(v -> {
            CharSequence[] options = {"בחירה מהגלריה", "צילום תמונה במצלמה"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("הוספת תמונה");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    galleryLauncher.launch(intent);
                } else {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(intent);
                }
            });
            builder.show();
        });

        cardReminder.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                    String timeFormatted = String.format("%02d:%02d", hourOfDay, minute);
                    String dateFormatted = String.format("%02d.%02d.%d", dayOfMonth, (month1 + 1), year1);
                    tvReminderTime.setText(dateFormatted + " - " + timeFormatted);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }, year, month, day);
            datePickerDialog.show();
        });

        // כפתור שמירה עם מניעת לחיצות כפולות
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String price = etPrice.getText().toString().trim();
            String link = etLink.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            String reminder = tvReminderTime.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(this, "נא להזין כותרת לפריט", Toast.LENGTH_SHORT).show();
            } else {
                // השבתת הכפתור מיד כדי למנוע שמירות כפולות בלחיצה מהירה
                btnSave.setEnabled(false);

                Map<String, Object> itemData = new HashMap<>();
                itemData.put("title", title);
                itemData.put("category", category);
                itemData.put("price", price);
                itemData.put("link", link);
                itemData.put("note", note);
                itemData.put("reminder", reminder);
                if (imageUri != null) {
                    itemData.put("imageUri", imageUri.toString());
                }

                db.collection("items")
                        .add(itemData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "הפריט נשמר בהצלחה בפיירבייס!", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("ITEM_TITLE", title);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // במקרה של שגיאה, מחזירים את האפשרות ללחוץ שוב
                            btnSave.setEnabled(true);
                            Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });
    }
}