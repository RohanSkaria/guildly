package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextNewPassword;
    private Button buttonCheckEmail, buttonResetPassword;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editTextEmail = findViewById(R.id.editTextEmailForgot);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        buttonCheckEmail = findViewById(R.id.buttonCheckEmail);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        editTextNewPassword.setEnabled(false);
        buttonResetPassword.setEnabled(false);

        buttonCheckEmail.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Enter your email");
                return;
            }

            String sanitizedEmail = email.replace(".", ",");
            usersRef.child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Email found. Enter new password.", Toast.LENGTH_SHORT).show();
                        editTextNewPassword.setEnabled(true);
                        buttonResetPassword.setEnabled(true);
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Email not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        buttonResetPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String newPassword = editTextNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(newPassword)) {
                editTextNewPassword.setError("Enter a new password");
                return;
            }

            String sanitizedEmail = email.replace(".", ",");
            usersRef.child(sanitizedEmail).child("password").setValue(newPassword)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ForgotPasswordActivity.this, "Password reset successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}