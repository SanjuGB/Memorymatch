package com.example.memorymatch;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton, goToLoginButton;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        goToLoginButton = findViewById(R.id.goToLoginButton);
        progressBar = findViewById(R.id.registerProgress);

        registerButton.setOnClickListener(v -> registerUser());
        goToLoginButton.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirm = confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Enter email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Enter password");
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = auth.getCurrentUser();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
