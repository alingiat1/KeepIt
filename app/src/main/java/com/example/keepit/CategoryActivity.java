package com.example.keepit;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CategoryActivity extends AppCompatActivity {

    private TextView categoryTitle;
    private View backButton;
    private ImageView topActionIcon;
    private Button addItemButton;
    private LinearLayout itemsContainer;
    private ActivityResultLauncher<Intent> addItemLauncher;

    private FirebaseFirestore db;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        db = FirebaseFirestore.getInstance();

        categoryTitle = findViewById(R.id.categoryTitle);
        backButton = findViewById(R.id.backButton);
        topActionIcon = findViewById(R.id.topActionIcon);
        addItemButton = findViewById(R.id.addItemButton);
        itemsContainer = findViewById(R.id.itemsContainer);

        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        if (categoryName != null) {
            categoryTitle.setText(categoryName);
        }

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        if (topActionIcon != null) {
            topActionIcon.setOnClickListener(v -> finish());
        }

        // טעינת הפריטים הקיימים מפיירבייס מיד כשהמסך נפתח
        loadItemsFromFirebase();

        addItemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // רענון הרשימה מהענן לאחר הוספת פריט חדש
                        loadItemsFromFirebase();
                    }
                }
        );

        addItemButton.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, AddItemActivity.class);
            intent.putExtra("CATEGORY_NAME", categoryName);
            addItemLauncher.launch(intent);
        });
    }

    // פונקציה ששואבת את כל הפריטים של הקטגוריה הזו מתוך פיירבייס ומציגה אותם
    private void loadItemsFromFirebase() {
        // מנקים את הרשימה הקיימת כדי שלא יהיו כפילויות בטעינה
        itemsContainer.removeAllViews();

        db.collection("items")
                .whereEqualTo("category", categoryName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String title = document.getString("title");
                        if (title != null) {
                            addNewItemCard(title);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת הפריטים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addNewItemCard(String titleText) {
        float density = getResources().getDisplayMetrics().density;

        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (85 * density)
        );
        cardParams.setMargins(0, 0, 0, (int) (12 * density));
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16 * density);
        cardView.setCardElevation(2 * density);
        cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setGravity(Gravity.CENTER_VERTICAL);
        int padding = (int) (12 * density);
        innerLayout.setPadding(padding, padding, padding, padding);

        ImageView img = new ImageView(this);
        int imgSize = (int) (60 * density);
        img.setLayoutParams(new LinearLayout.LayoutParams(imgSize, imgSize));
        img.setBackgroundColor(Color.parseColor("#FFE4EC"));
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        innerLayout.addView(img);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        textParams.setMargins((int) (16 * density), 0, 0, 0);
        tv.setLayoutParams(textParams);
        tv.setText(titleText);
        tv.setTextColor(Color.parseColor("#333333"));
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        innerLayout.addView(tv);

        ImageView star = new ImageView(this);
        int starSize = (int) (24 * density);
        star.setLayoutParams(new LinearLayout.LayoutParams(starSize, starSize));
        star.setImageResource(android.R.drawable.btn_star);
        innerLayout.addView(star);

        cardView.addView(innerLayout);
        itemsContainer.addView(cardView);
    }
}