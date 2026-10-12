package com.example.keepit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // הגדרת שוליים למסך
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // חיבור רכיבים מה-XML
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        // כפתור התחברות
        loginButton.setOnClickListener(v -> {
            String email = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();

                                // מעבר למסך הבית לאחר התחברות מוצלחת
                                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "ההתחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // כפתור פתיחת חלון הרשמה
        registerButton.setOnClickListener(v -> {
            showRegisterDialog();
        });
    }

    // פונקציה להצגת החלון הקופץ להרשמה
    private void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialogsingup, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText dUsername = dialogView.findViewById(R.id.dialogUsernameInput);
        EditText dEmail = dialogView.findViewById(R.id.dialogEmailInput);
        EditText dPassword = dialogView.findViewById(R.id.dialogPasswordInput);
        EditText dConfirmPassword = dialogView.findViewById(R.id.dialogConfirmPasswordInput);
        Button dRegisterBtn = dialogView.findViewById(R.id.dialogRegisterButton);

        dRegisterBtn.setOnClickListener(v -> {
            String username = dUsername.getText().toString().trim();
            String email = dEmail.getText().toString().trim();
            String password = dPassword.getText().toString().trim();
            String confirmPassword = dConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Toast.makeText(MainActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(MainActivity.this, "הסיסמאות אינן תואמות", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(MainActivity.this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "המשתמש נרשם בהצלחה!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();

                                // מעבר למסך הבית לאחר רישום מוצלח
                                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "ההרשמה נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        dialog.show();
    }
}