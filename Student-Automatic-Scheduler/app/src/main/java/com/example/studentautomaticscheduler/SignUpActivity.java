package com.example.studentautomaticscheduler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etEmail    = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnSignUp    = findViewById(R.id.btnSignUp);
        TextView txtLogin   = findViewById(R.id.txtLogin);

        btnSignUp.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save account (temporary)
            SharedPreferences prefs = getSharedPreferences("USER_DATA", MODE_PRIVATE);
            prefs.edit()
                    .putString("USERNAME", username)
                    .putString("EMAIL", email)
                    .putString("PASSWORD", password)
                    .apply();

            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();

            // Go back to Login
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        // Already have an account? Login
        txtLogin.setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class))
        );
    }
}
