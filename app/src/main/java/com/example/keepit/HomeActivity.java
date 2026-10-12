package com.example.keepit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    private CardView cardShopping, cardPlaces, cardGifts, cardIdeas, cardRecipes, addCategoryCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // חיבור כל הכרטיסיות מתוך קובץ ה-XML של מסך הבית
        cardShopping = findViewById(R.id.cardShopping);
        cardPlaces = findViewById(R.id.cardPlaces);
        cardGifts = findViewById(R.id.cardGifts);
        cardIdeas = findViewById(R.id.cardIdeas);
        cardRecipes = findViewById(R.id.cardRecipes);
        addCategoryCard = findViewById(R.id.addCategoryCard);

        // הגדרת מעבר דינמי למסך קטגוריה לפי הקטגוריה שנלחצה
        cardShopping.setOnClickListener(v -> openCategory("לקנות"));
        cardPlaces.setOnClickListener(v -> openCategory("מקומות"));
        cardGifts.setOnClickListener(v -> openCategory("מתנות"));
        cardIdeas.setOnClickListener(v -> openCategory("רעיונות"));
        cardRecipes.setOnClickListener(v -> openCategory("מתכונים"));

        // כפתור הוספת קטגוריה חדשה
        addCategoryCard.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "כאן ייפתח חלון הוספת קטגוריה חדשה", Toast.LENGTH_SHORT).show();
        });
    }

    // פונקציה עזר שפותחת את מסך הקטגוריות הדינמי ומעבירה אליו את שם הקטגוריה
    private void openCategory(String categoryName) {
        Intent intent = new Intent(HomeActivity.this, CategoryActivity.class);
        intent.putExtra("CATEGORY_NAME", categoryName);
        startActivity(intent);
    }
}