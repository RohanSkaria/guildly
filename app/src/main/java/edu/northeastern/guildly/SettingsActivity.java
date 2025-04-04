package edu.northeastern.guildly;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.northeastern.guildly.data.User;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextCurrentEmail;
    private EditText editTextNewEmail;
    private EditText editTextCurrentPassword;
    private EditText editTextNewPassword;
    private Button buttonUpdateEmail;
    private Button buttonUpdatePassword;
    private Button buttonLogout;
    private ImageView backButton;

    private DatabaseReference usersRef;
    private String userKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Settings");


        editTextCurrentEmail = findViewById(R.id.editTextCurrentEmail);
        editTextNewEmail = findViewById(R.id.editTextNewEmail);
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        buttonUpdateEmail = findViewById(R.id.buttonUpdateEmail);
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword);
        buttonLogout = findViewById(R.id.buttonLogout);
        backButton = findViewById(R.id.backButton);


        String currentEmail = MainActivity.currentUserEmail;
        if (currentEmail == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userKey = currentEmail.replace(".", ",");
        usersRef = FirebaseDatabase.getInstance().getReference("users");


        editTextCurrentEmail.setText(currentEmail);

        backButton.setOnClickListener(v -> finish());

        buttonUpdateEmail.setOnClickListener(v -> updateEmail());

        buttonUpdatePassword.setOnClickListener(v -> updatePassword());

        buttonLogout.setOnClickListener(v -> logout());
    }

    private void updateEmail() {
        String currentEmail = editTextCurrentEmail.getText().toString().trim();
        String newEmail = editTextNewEmail.getText().toString().trim();

        if (TextUtils.isEmpty(newEmail)) {
            editTextNewEmail.setError("Please enter a new email");
            return;
        }

        if (currentEmail.equals(newEmail)) {
            editTextNewEmail.setError("New email must be different from current email");
            return;
        }


        String newUserKey = newEmail.replace(".", ",");
        usersRef.child(newUserKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    editTextNewEmail.setError("Email is already in use");
                } else {

                    usersRef.child(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {

                                user.email = newEmail;


                                usersRef.child(newUserKey).setValue(user)
                                        .addOnSuccessListener(aVoid -> {

                                            usersRef.child(userKey).removeValue();


                                            MainActivity.currentUserEmail = newEmail;


                                            editTextCurrentEmail.setText(newEmail);
                                            editTextNewEmail.setText("");

                                            Toast.makeText(SettingsActivity.this,
                                                    "Email updated successfully",
                                                    Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SettingsActivity.this,
                                                    "Failed to update email: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e("SettingsActivity", "Failed to read user data", error.toException());
                            Toast.makeText(SettingsActivity.this,
                                    "Error: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("SettingsActivity", "Failed to check if email exists", error.toException());
                Toast.makeText(SettingsActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword() {
        String currentPassword = editTextCurrentPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            editTextCurrentPassword.setError("Please enter your current password");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            editTextNewPassword.setError("Please enter a new password");
            return;
        }

        // Add a logging statement for debugging
        Log.d(TAG, "Attempting to update password for user: " + userKey);


        usersRef.child(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {


                    if (currentPassword.equals(user.password)) {
                        // Update password with log messages and detailed error handling
                        Log.d(TAG, "Password verified, updating to new password");

                        usersRef.child(userKey).child("password").setValue(newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Password updated successfully in Firebase");
                                    editTextCurrentPassword.setText("");
                                    editTextNewPassword.setText("");
                                    Toast.makeText(SettingsActivity.this,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating password: " + e.getMessage(), e);
                                    Toast.makeText(SettingsActivity.this,
                                               "Failed to update password: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Log.d(TAG, "Current password does not match stored password");
                        editTextCurrentPassword.setError("Current password is incorrect");
                    }
                } else {
                    Log.e(TAG, "User data is null for key: " + userKey);
                    Toast.makeText(SettingsActivity.this,
                            "Error: User data not found",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to read user data: " + error.getMessage(), error.toException());
                Toast.makeText(SettingsActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void logout() {

        SharedPreferences prefs = getSharedPreferences("GuildlyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("loggedInUserEmail");
        editor.apply();

        MainActivity.currentUserEmail = null;

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}