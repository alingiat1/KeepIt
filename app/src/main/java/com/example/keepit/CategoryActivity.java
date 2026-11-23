
package com.example.keepit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;

public class CategoryActivity extends AppCompatActivity {

    private TextView categoryTitle;
    private View backButton;
    private ImageView topActionIcon;
    private Button addItemButton;
    private LinearLayout itemsContainer;

    private ActivityResultLauncher<Intent> itemLauncher;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        categoryTitle = findViewById(R.id.categoryTitle);
        backButton = findViewById(R.id.backButton);
        topActionIcon = findViewById(R.id.topActionIcon);
        addItemButton = findViewById(R.id.addItemButton);
        itemsContainer = findViewById(R.id.itemsContainer);

        categoryName = getIntent().getStringExtra("CATEGORY_NAME");

        if (categoryName != null) {
            categoryTitle.setText(categoryName);
        }

        itemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> loadItemsFromFirebase()
        );

        backButton.setOnClickListener(v -> finish());

        if (topActionIcon != null) {
            topActionIcon.setOnClickListener(v -> finish());
        }

        addItemButton.setOnClickListener(v -> openAddItemScreen());

        loadItemsFromFirebase();
    }

    private void openAddItemScreen() {
        Intent intent = new Intent(
                CategoryActivity.this,
                AddItemActivity.class
        );

        intent.putExtra("CATEGORY_NAME", categoryName);
        itemLauncher.launch(intent);
    }

    private void loadItemsFromFirebase() {

        itemsContainer.removeAllViews();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "יש להתחבר כדי לראות פריטים",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (categoryName == null || categoryName.isEmpty()) {
            Toast.makeText(
                    this,
                    "לא נבחרה קטגוריה",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        db.collection("items")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("category", categoryName)
                .get()
                .addOnSuccessListener(snapshot -> {

                    itemsContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : snapshot) {

                        String title = document.getString("title");
                        String imageFileName =
                                document.getString("imageFileName");

                        if (title != null) {
                            addNewItemCard(
                                    title,
                                    imageFileName,
                                    document.getId()
                            );
                        }
                    }

                    if (snapshot.isEmpty()) {
                        Toast.makeText(
                                this,
                                "עדיין אין פריטים בקטגוריה הזאת",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "שגיאה בטעינת הפריטים: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void addNewItemCard(
            String titleText,
            String imageFileName,
            String itemId
    ) {

        float density = getResources()
                .getDisplayMetrics().density;

        CardView cardView = new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (int) (85 * density)
                );

        cardParams.setMargins(
                0, 0, 0, (int) (12 * density)
        );

        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16 * density);
        cardView.setCardElevation(2 * density);
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setClickable(true);
        cardView.setFocusable(true);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setGravity(Gravity.CENTER_VERTICAL);

        int padding = (int) (12 * density);
        innerLayout.setPadding(padding, padding, padding, padding);

        ImageView img = new ImageView(this);
        int imgSize = (int) (60 * density);

        img.setLayoutParams(
                new LinearLayout.LayoutParams(imgSize, imgSize)
        );

        img.setBackgroundColor(Color.parseColor("#FFE4EC"));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(android.R.drawable.ic_menu_gallery);

        if (imageFileName != null && !imageFileName.isEmpty()) {

            File imageFile = new File(
                    new File(getFilesDir(), "item_images"),
                    imageFileName
            );

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(
                        imageFile.getAbsolutePath()
                );

                if (bitmap != null) {
                    img.setImageBitmap(bitmap);
                }
            }
        }

        innerLayout.addView(img);

        TextView tv = new TextView(this);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                );

        textParams.setMargins(
                (int) (16 * density), 0, 0, 0
        );

        tv.setLayoutParams(textParams);
        tv.setText(titleText);
        tv.setTextColor(Color.parseColor("#333333"));
        tv.setTextSize(16);
        tv.setTypeface(null, Typeface.BOLD);

        innerLayout.addView(tv);

        ImageView star = new ImageView(this);
        int starSize = (int) (24 * density);

        star.setLayoutParams(
                new LinearLayout.LayoutParams(starSize, starSize)
        );

        star.setImageResource(android.R.drawable.btn_star);
        innerLayout.addView(star);

        cardView.addView(innerLayout);

        cardView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CategoryActivity.this,
                    ItemDetailsActivity.class
            );

            intent.putExtra("ITEM_ID", itemId);
            itemLauncher.launch(intent);
        });

        itemsContainer.addView(cardView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItemsFromFirebase();
    }
}
