package com.example.keepit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class ItemDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String itemId;
    private String imageFileName;

    private ImageView itemImage;
    private LinearLayout detailsContainer;

    private ActivityResultLauncher<Intent> editLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        itemId = getIntent().getStringExtra("ITEM_ID");

        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadItemDetails();
                    }
                }
        );

        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(
                    this,
                    "לא נמצא פריט",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        createScreen();
        loadItemDetails();
    }

    private void createScreen() {

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#FFF5F8"));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(20), dp(20), dp(20), dp(20));

        scrollView.addView(mainLayout);
        setContentView(scrollView);

        Button backButton = new Button(this);
        backButton.setText("חזרה");
        mainLayout.addView(backButton);

        backButton.setOnClickListener(v -> finish());

        itemImage = new ImageView(this);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(220)
                );

        imageParams.setMargins(0, dp(15), 0, dp(20));

        itemImage.setLayoutParams(imageParams);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        itemImage.setBackgroundColor(Color.parseColor("#FFE4EC"));
        itemImage.setImageResource(android.R.drawable.ic_menu_gallery);

        mainLayout.addView(itemImage);

        detailsContainer = new LinearLayout(this);
        detailsContainer.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(detailsContainer);

        Button editButton = new Button(this);
        editButton.setText("עריכת פריט");
        editButton.setTextColor(Color.WHITE);
        editButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#D81B60")
                )
        );

        mainLayout.addView(editButton);

        editButton.setOnClickListener(v -> openEditScreen());

        Button deleteButton = new Button(this);
        deleteButton.setText("מחיקת פריט");
        deleteButton.setTextColor(Color.WHITE);
        deleteButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#B71C1C")
                )
        );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        deleteParams.setMargins(0, dp(10), 0, dp(20));
        deleteButton.setLayoutParams(deleteParams);

        mainLayout.addView(deleteButton);

        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadItemDetails() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        detailsContainer.removeAllViews();

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
                                "אין הרשאה לצפות בפריט הזה",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                        return;
                    }

                    imageFileName =
                            document.getString("imageFileName");

                    showImage();

                    addDetail("שם הפריט", document.getString("title"));
                    addDetail("קטגוריה", document.getString("category"));
                    addDetail("מחיר", document.getString("price"));
                    addDetail("קישור", document.getString("link"));
                    addDetail("הערות", document.getString("note"));
                    addDetail("תזכורת", document.getString("reminder"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "שגיאה בטעינת הפריט: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showImage() {

        itemImage.setImageResource(android.R.drawable.ic_menu_gallery);

        if (imageFileName == null || imageFileName.isEmpty()) {
            return;
        }

        File imageFile = new File(
                new File(getFilesDir(), "item_images"),
                imageFileName
        );

        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(
                    imageFile.getAbsolutePath()
            );

            if (bitmap != null) {
                itemImage.setImageBitmap(bitmap);
            }
        }
    }

    private void addDetail(String label, String value) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        TextView textView = new TextView(this);

        textView.setText(label + "\n" + value);
        textView.setTextColor(Color.parseColor("#333333"));
        textView.setTextSize(17);
        textView.setPadding(dp(14), dp(12), dp(14), dp(12));
        textView.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, dp(10));
        textView.setLayoutParams(params);

        detailsContainer.addView(textView);
    }

    private void openEditScreen() {

        Intent intent = new Intent(
                ItemDetailsActivity.this,
                AddItemActivity.class
        );

        intent.putExtra("ITEM_ID", itemId);
        editLauncher.launch(intent);
    }

    private void confirmDelete() {

        new AlertDialog.Builder(this)
                .setTitle("מחיקת פריט")
                .setMessage("את בטוחה שאת רוצה למחוק את הפריט?")
                .setNegativeButton(
                        "ביטול",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "מחיקה",
                        (dialog, which) -> deleteItem()
                )
                .show();
    }

    private void deleteItem() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        finish();
                        return;
                    }

                    String ownerId = document.getString("userId");

                    if (ownerId == null ||
                            !ownerId.equals(user.getUid())) {

                        Toast.makeText(
                                this,
                                "אין הרשאה למחוק את הפריט",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String savedImage =
                            document.getString("imageFileName");

                    db.collection("items")
                            .document(itemId)
                            .delete()
                            .addOnSuccessListener(unused -> {

                                if (savedImage != null) {
                                    File imageFile = new File(
                                            new File(getFilesDir(), "item_images"),
                                            savedImage
                                    );

                                    if (imageFile.exists()) {
                                        imageFile.delete();
                                    }
                                }

                                Toast.makeText(
                                        this,
                                        "הפריט נמחק",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "המחיקה נכשלה: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "שגיאה בבדיקת הפריט: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
