
package com.example.keepit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class FavoritesActivity extends AppCompatActivity {

    private LinearLayout favoritesContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        createScreen();
    }

    private void createScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#FFF5F8"));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        setContentView(root);

        // כותרת המסך
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(12));

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(70)
                )
        );

        TextView backButton = new TextView(this);
        backButton.setText("‹");
        backButton.setTextSize(38);
        backButton.setTextColor(Color.parseColor("#57172D"));
        backButton.setGravity(Gravity.CENTER);

        header.addView(
                backButton,
                new LinearLayout.LayoutParams(dp(45), dp(45))
        );

        backButton.setOnClickListener(v -> finish());

        TextView title = new TextView(this);
        title.setText("המועדפים שלי ♡");
        title.setTextSize(27);
        title.setTextColor(Color.parseColor("#57172D"));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        header.addView(title, titleParams);

        // אזור גלילה
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        favoritesContainer = new LinearLayout(this);
        favoritesContainer.setOrientation(LinearLayout.VERTICAL);
        favoritesContainer.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(24)
        );

        scrollView.addView(favoritesContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {

        if (auth.getCurrentUser() == null) {
            showEmptyMessage("צריך להתחבר כדי לראות את המועדפים");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("items")
                .whereEqualTo("userId", userId)
                .whereEqualTo("favorite", true)
                .get()
                .addOnSuccessListener(snapshot -> {

                    favoritesContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        showEmptyMessage(
                                "עדיין אין לך מועדפים ♡\n" +
                                        "סמני לב על פריטים שאת אוהבת"
                        );
                        return;
                    }

                    LinearLayout row = null;
                    int index = 0;

                    for (DocumentSnapshot item :
                            snapshot.getDocuments()) {

                        if (index % 2 == 0) {

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

                            favoritesContainer.addView(
                                    row,
                                    rowParams
                            );
                        }

                        FrameLayout card = createFavoriteCard(item);

                        LinearLayout.LayoutParams cardParams =
                                new LinearLayout.LayoutParams(
                                        0,
                                        dp(225),
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

                        index++;
                    }

                    if (index % 2 != 0 && row != null) {
                        View spacer = new View(this);

                        row.addView(
                                spacer,
                                new LinearLayout.LayoutParams(
                                        0,
                                        dp(225),
                                        1
                                )
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "לא הצלחנו לטעון את המועדפים",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private FrameLayout createFavoriteCard(
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

        // תמונת הפריט
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(
                Color.parseColor("#F4D6E1")
        );

        card.addView(
                image,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        String imageFileName =
                item.getString("imageFileName");

        if (imageFileName != null &&
                !imageFileName.isEmpty()) {

            File imageFile = new File(
                    new File(getFilesDir(), "item_images"),
                    imageFileName
            );

            if (imageFile.exists()) {
                image.setImageURI(Uri.fromFile(imageFile));
            }
        }

        // אזור ורוד בתחתית
        View bottomOverlay = new View(this);

        GradientDrawable overlay =
                new GradientDrawable();

        overlay.setColor(
                Color.parseColor("#FFFCE6ED")
        );

        bottomOverlay.setBackground(overlay);

        FrameLayout.LayoutParams overlayParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(58),
                        Gravity.BOTTOM
                );

        card.addView(bottomOverlay, overlayParams);

        // שם הפריט
        TextView itemTitle = new TextView(this);

        String name = item.getString("title");

        itemTitle.setText(
                name == null ? "פריט ללא שם" : name
        );

        itemTitle.setTextColor(
                Color.parseColor("#57172D")
        );

        itemTitle.setTextSize(16);
        itemTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        itemTitle.setGravity(
                Gravity.CENTER_VERTICAL | Gravity.END
        );

        itemTitle.setPadding(
                dp(10),
                0,
                dp(12),
                0
        );

        itemTitle.setMaxLines(2);

        FrameLayout.LayoutParams titleParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(58),
                        Gravity.BOTTOM
                );

        card.addView(itemTitle, titleParams);

        // לב להסרה מהמועדפים
        TextView heart = new TextView(this);
        heart.setText("♥");
        heart.setTextColor(
                Color.parseColor("#C83E6D")
        );
        heart.setTextSize(25);
        heart.setGravity(Gravity.CENTER);

        GradientDrawable heartBackground =
                new GradientDrawable();

        heartBackground.setColor(Color.WHITE);
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

        heartParams.setMargins(dp(10), dp(10), 0, 0);

        card.addView(heart, heartParams);

        heart.setOnClickListener(v ->
                db.collection("items")
                        .document(item.getId())
                        .update("favorite", false)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(
                                    FavoritesActivity.this,
                                    "הוסר מהמועדפים",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadFavorites();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(
                                        FavoritesActivity.this,
                                        "לא הצלחנו להסיר את הפריט",
                                        Toast.LENGTH_SHORT
                                ).show()
                        )
        );

        // לחיצה על הכרטיס פותחת את פרטי הפריט
        card.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FavoritesActivity.this,
                    ItemDetailsActivity.class
            );

            intent.putExtra("ITEM_ID", item.getId());

            startActivity(intent);
        });

        return card;
    }

    private void showEmptyMessage(String message) {

        favoritesContainer.removeAllViews();

        TextView emptyText = new TextView(this);

        emptyText.setText(message);
        emptyText.setTextColor(
                Color.parseColor("#8B5268")
        );

        emptyText.setTextSize(19);
        emptyText.setGravity(Gravity.CENTER);

        emptyText.setPadding(
                dp(20),
                dp(60),
                dp(20),
                dp(60)
        );

        favoritesContainer.addView(emptyText);
    }
}
