package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private Button buttonSendEmail;
    private ImageButton buttonBack;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editTextEmail = findViewById(R.id.editTextEmailForgot);
        buttonSendEmail = findViewById(R.id.buttonSendResetEmail);
        buttonBack = findViewById(R.id.buttonBackForgot);
        firebaseAuth = FirebaseAuth.getInstance();

        buttonBack.setOnClickListener(v -> {
            finish();
        });

        buttonSendEmail.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Enter your email");
                return;
            }

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(ForgotPasswordActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
