package edu.northeastern.guildly;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.northeastern.guildly.data.User;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextCurrentPassword;
    private EditText editTextNewPassword;
    private ImageView toggleCurrentPassword;
    private ImageView toggleNewPassword;
    private Button buttonUpdatePassword;
    private Button buttonLogout;
    private ImageView backButton;

    private boolean isCurrentVisible = false;
    private boolean isNewVisible = false;

    private DatabaseReference usersRef;
    private String userKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setTitle("Settings");

        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        toggleCurrentPassword = findViewById(R.id.toggleCurrentPassword);
        toggleNewPassword = findViewById(R.id.toggleNewPassword);
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword);
        buttonLogout = findViewById(R.id.buttonLogout);
        backButton = findViewById(R.id.backButton);

        toggleCurrentPassword.setOnClickListener(v -> {
            isCurrentVisible = !isCurrentVisible;
            if (isCurrentVisible) {
                editTextCurrentPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleCurrentPassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                editTextCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleCurrentPassword.setImageResource(R.drawable.ic_eye);
            }
            editTextCurrentPassword.setSelection(editTextCurrentPassword.getText().length());
        });

        toggleNewPassword.setOnClickListener(v -> {
            isNewVisible = !isNewVisible;
            if (isNewVisible) {
                editTextNewPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleNewPassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                editTextNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleNewPassword.setImageResource(R.drawable.ic_eye);
            }
            editTextNewPassword.setSelection(editTextNewPassword.getText().length());
        });

        String currentEmail = MainActivity.currentUserEmail;
        if (currentEmail == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userKey = currentEmail.replace(".", ",");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        backButton.setOnClickListener(v -> finish());

        buttonUpdatePassword.setOnClickListener(v -> updatePassword());

        buttonLogout.setOnClickListener(v -> logout());
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

        usersRef.child(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (currentPassword.equals(user.password)) {
                        usersRef.child(userKey).child("password").setValue(newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    editTextCurrentPassword.setText("");
                                    editTextNewPassword.setText("");
                                    Toast.makeText(SettingsActivity.this,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SettingsActivity.this,
                                            "Failed to update password: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        editTextCurrentPassword.setError("Current password is incorrect");
                    }
                } else {
                    Toast.makeText(SettingsActivity.this,
                            "Error: User data not found",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SettingsActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("GuildlyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("loggedInUserEmail");
        editor.apply();
        MainActivity.currentUserEmail = null;
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.putExtra("logout", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}