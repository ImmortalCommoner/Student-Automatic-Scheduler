package com.example.studentautomaticscheduler;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView txtSignUp = findViewById(R.id.txtSignUp);

        // ---- LOGIN BUTTON ----
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (user.equals("User") && pass.equals("Passwordkey")) {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("USERNAME", user);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ---- SIGN UP TEXT ----
        setupSignUpText(txtSignUp);
    }

    private void setupSignUpText(TextView txtSignUp) {
        String fullText = "Don’t have an account? Sign up";
        SpannableString span = new SpannableString(fullText);

        int start = fullText.indexOf("Sign up");
        int end = start + "Sign up".length();

        span.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        span.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        txtSignUp.setText(span);

        txtSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });
    }
}
