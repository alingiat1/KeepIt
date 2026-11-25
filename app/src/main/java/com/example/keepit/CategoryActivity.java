
package com.example.keepit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private LinearLayout itemsContainer;
    private Button addItemButton;

    private TextView filterAll;
    private TextView filterFavorites;

    private String categoryName;
    private boolean showingFavorites = false;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<DocumentSnapshot> itemList =
            new ArrayList<>();

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        itemsContainer = findViewById(R.id.itemsContainer);
        addItemButton = findViewById(R.id.addItemButton);

        filterAll = findViewById(R.id.filterAll);
        filterFavorites = findViewById(R.id.filterFavorites);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        categoryName =
                getIntent().getStringExtra("CATEGORY_NAME");

        TextView categoryTitle =
                findViewById(R.id.categoryTitle);

        if (categoryName != null &&
                !categoryName.isEmpty()) {
            categoryTitle.setText(categoryName + " ♡");
        }

        findViewById(R.id.backButton)
                .setOnClickListener(v -> finish());

        findViewById(R.id.topActionIcon)
                .setOnClickListener(v ->
                        Toast.makeText(
                                this,
                                categoryName,
                                Toast.LENGTH_SHORT
                        ).show()
                );

        setupAddButton();
        setupFilterButtons();

        addItemButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    CategoryActivity.this,
                    AddItemActivity.class
            );

            intent.putExtra(
                    "CATEGORY_NAME",
                    categoryName
            );

            startActivityForResult(intent, 100);
        });
    }

    private void setupAddButton() {

        GradientDrawable pinkBackground =
                new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{
                                Color.parseColor("#D84C78"),
                                Color.parseColor("#B92F5D")
                        }
                );

        pinkBackground.setCornerRadius(dp(40));

        addItemButton.setBackgroundTintList(null);

        ViewCompat.setBackgroundTintList(
                addItemButton,
                null
        );

        addItemButton.setBackground(pinkBackground);

        addItemButton.setBackgroundTintList(null);

        ViewCompat.setBackgroundTintList(
                addItemButton,
                null
        );

        addItemButton.setTextColor(Color.WHITE);
        addItemButton.setTextSize(20);

        addItemButton.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        addItemButton.setAllCaps(false);
        addItemButton.setText("+  הוספת פריט");
        addItemButton.setElevation(dp(4));
    }

    private void setupFilterButtons() {

        filterAll.setOnClickListener(v -> {
            showingFavorites = false;
            updateFilterStyle();
            showItems();
        });

        filterFavorites.setOnClickListener(v -> {
            showingFavorites = true;
            updateFilterStyle();
            showItems();
        });

        updateFilterStyle();
    }

    private void updateFilterStyle() {

        if (showingFavorites) {
            setSelectedFilter(filterFavorites);
            setUnselectedFilter(filterAll);
        } else {
            setSelectedFilter(filterAll);
            setUnselectedFilter(filterFavorites);
        }
    }

    private void setSelectedFilter(TextView filter) {

        GradientDrawable background =
                new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{
                                Color.parseColor("#D84C78"),
                                Color.parseColor("#B92F5D")
                        }
                );

        background.setCornerRadius(dp(30));

        filter.setBackground(background);
        filter.setTextColor(Color.WHITE);
        filter.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
    }

    private void setUnselectedFilter(TextView filter) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.parseColor("#FFFFE9F0")
        );

        background.setCornerRadius(dp(30));

        filter.setBackground(background);

        filter.setTextColor(
                Color.parseColor("#8B5268")
        );

        filter.setTypeface(
                Typeface.DEFAULT,
                Typeface.NORMAL
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == 100) {
            loadItems();
        }
    }

    private void loadItems() {

        if (auth.getCurrentUser() == null ||
                categoryName == null) {
            return;
        }

        String userId =
                auth.getCurrentUser().getUid();

        db.collection("items")
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", categoryName)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    itemList.clear();

                    itemList.addAll(
                            querySnapshot.getDocuments()
                    );

                    showItems();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "לא הצלחנו לטעון את הפריטים",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void showItems() {

        itemsContainer.removeAllViews();

        List<DocumentSnapshot> displayedItems =
                new ArrayList<>();

        for (DocumentSnapshot item : itemList) {

            boolean isFavorite =
                    Boolean.TRUE.equals(
                            item.getBoolean("favorite")
                    );

            if (!showingFavorites || isFavorite) {
                displayedItems.add(item);
            }
        }

        if (displayedItems.isEmpty()) {

            TextView emptyText = new TextView(this);

            if (showingFavorites) {
                emptyText.setText(
                        "עדיין אין לך מועדפים ♡\n" +
                                "סמני פריטים בלב כדי לשמור אותם כאן"
                );
            } else {
                emptyText.setText(
                        "עדיין אין כאן פריטים ♡\n" +
                                "הוסיפי את הראשון שלך"
                );
            }

            emptyText.setTextColor(
                    Color.parseColor("#8B5268")
            );

            emptyText.setTextSize(19);
            emptyText.setGravity(Gravity.CENTER);

            emptyText.setPadding(
                    dp(20),
                    dp(50),
                    dp(20),
                    dp(50)
            );

            itemsContainer.addView(emptyText);
            return;
        }

        LinearLayout row = null;

        for (int i = 0; i < displayedItems.size(); i++) {

            if (i % 2 == 0) {

                row = new LinearLayout(this);

                row.setOrientation(
                        LinearLayout.HORIZONTAL
                );

                row.setLayoutDirection(
                        View.LAYOUT_DIRECTION_RTL
                );

                LinearLayout.LayoutParams rowParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                rowParams.bottomMargin = dp(14);

                itemsContainer.addView(
                        row,
                        rowParams
                );
            }

            DocumentSnapshot item =
                    displayedItems.get(i);

            FrameLayout card = createItemCard(item);

            int cardHeight =
                    (i % 3 == 0) ? 235 : 205;

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(cardHeight),
                            1
                    );

            cardParams.setMargins(
                    dp(5),
                    0,
                    dp(5),
                    0
            );

            if (row != null) {
                row.addView(card, cardParams);
            }
        }

        if (displayedItems.size() % 2 != 0 &&
                row != null) {

            View spacer = new View(this);

            row.addView(
                    spacer,
                    new LinearLayout.LayoutParams(
                            0,
                            dp(205),
                            1
                    )
            );
        }
    }

    private FrameLayout createItemCard(
            DocumentSnapshot item) {

        FrameLayout card = new FrameLayout(this);

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.parseColor("#FFF9FB")
        );

        cardBackground.setCornerRadius(dp(24));

        card.setBackground(cardBackground);
        card.setClipToOutline(true);
        card.setElevation(dp(3));

        ImageView image = new ImageView(this);

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setBackgroundColor(
                Color.parseColor("#F4D6E1")
        );

        FrameLayout.LayoutParams imageParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

        card.addView(image, imageParams);

        String imageFileName =
                item.getString("imageFileName");

        if (imageFileName != null &&
                !imageFileName.isEmpty()) {

            File imageFile = new File(
                    new File(
                            getFilesDir(),
                            "item_images"
                    ),
                    imageFileName
            );

            if (imageFile.exists()) {
                image.setImageURI(
                        Uri.fromFile(imageFile)
                );
            }
        }

        View bottomOverlay = new View(this);

        GradientDrawable overlayBackground =
                new GradientDrawable();

        overlayBackground.setColor(
                Color.parseColor("#FFFCE6ED")
        );

        overlayBackground.setCornerRadii(
                new float[]{
                        dp(18), dp(18),
                        dp(18), dp(18),
                        0, 0,
                        0, 0
                }
        );

        bottomOverlay.setBackground(
                overlayBackground
        );

        FrameLayout.LayoutParams overlayParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(54),
                        Gravity.BOTTOM
                );

        card.addView(
                bottomOverlay,
                overlayParams
        );

        TextView title = new TextView(this);

        String itemTitle =
                item.getString("title");

        title.setText(
                itemTitle == null ?
                        "פריט ללא שם" :
                        itemTitle
        );

        title.setTextColor(
                Color.parseColor("#57172D")
        );

        title.setTextSize(17);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER_VERTICAL |
                        Gravity.END
        );

        title.setPadding(
                dp(12),
                0,
                dp(14),
                0
        );

        title.setMaxLines(2);

        FrameLayout.LayoutParams titleParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(54),
                        Gravity.BOTTOM
                );

        card.addView(title, titleParams);

        boolean isFavorite =
                Boolean.TRUE.equals(
                        item.getBoolean("favorite")
                );

        TextView heart = new TextView(this);

        heart.setText(
                isFavorite ? "♥" : "♡"
        );

        heart.setTextColor(
                Color.parseColor("#C83E6D")
        );

        heart.setTextSize(25);
        heart.setGravity(Gravity.CENTER);
        heart.setClickable(true);
        heart.setFocusable(true);

        GradientDrawable heartBackground =
                new GradientDrawable();

        heartBackground.setColor(
                Color.parseColor("#FFFFF4F7")
        );

        heartBackground.setShape(
                GradientDrawable.OVAL
        );

        heart.setBackground(heartBackground);

        FrameLayout.LayoutParams heartParams =
                new FrameLayout.LayoutParams(
                        dp(42),
                        dp(42),
                        Gravity.TOP | Gravity.START
                );

        heartParams.setMargins(
                dp(10),
                dp(10),
                0,
                0
        );

        card.addView(heart, heartParams);

        heart.setOnClickListener(v -> {

            if (auth.getCurrentUser() == null) {
                return;
            }

            boolean newFavorite = !isFavorite;

            db.collection("items")
                    .document(item.getId())
                    .update("favorite", newFavorite)
                    .addOnSuccessListener(unused -> {

                        Toast.makeText(
                                CategoryActivity.this,
                                newFavorite ?
                                        "נוסף למועדפים ♡" :
                                        "הוסר מהמועדפים",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadItems();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    CategoryActivity.this,
                                    "לא הצלחנו לעדכן מועדפים",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
        });

        card.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CategoryActivity.this,
                    ItemDetailsActivity.class
            );

            intent.putExtra(
                    "ITEM_ID",
                    item.getId()
            );

            startActivity(intent);
        });

        return card;
    }
}
