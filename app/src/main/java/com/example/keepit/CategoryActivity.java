package com.example.keepit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {

    private TextView categoryTitle;
    private ImageView backButton;
    private Button addItemButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryTitle = findViewById(R.id.categoryTitle);
        backButton = findViewById(R.id.backButton);
        addItemButton = findViewById(R.id.addItemButton);

        // קבלת שם הקטגוריה שנשלחה ממסך הבית
        String categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        if (categoryName != null) {
            categoryTitle.setText(categoryName);
        }

        // כפתור חזור שמחזיר למסך הבית בצורה מפורשת
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        // כפתור הוספת פריט
        addItemButton.setOnClickListener(v -> {
            Toast.makeText(this, "כאן ייפתח חלון הוספת פריט ל-" + categoryName, Toast.LENGTH_SHORT).show();
        });
    }
}