package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        usernameEditText = findViewById(R.id.editTextUsername);
        loginButton = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        // Login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        String username = usernameEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Check if username exists in the database
        mDatabase.child("users").orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        progressBar.setVisibility(View.GONE);

                        if (dataSnapshot.exists()) {
                            // User exists - get the user ID (key)
                            String userId = null;
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                userId = userSnapshot.getKey();
                                break; // We only need the first one if multiple exist
                            }

                            // Store the user ID in shared preferences or similar for future reference
                            storeUserSession(userId, username);

                            // Navigate to home screen
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // User doesn't exist - start new user creation flow
                            Intent intent = new Intent(LoginActivity.this, HabitPickerActivity.class);
                            intent.putExtra("username", username);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this,
                                "Database error: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void storeUserSession(String userId, String username) {
        // You could use SharedPreferences to store the user's login state
        getSharedPreferences("GuildlyPrefs", MODE_PRIVATE)
                .edit()
                .putString("USER_ID", userId)
                .putString("USERNAME", username)
                .putBoolean("IS_LOGGED_IN", true)
                .apply();
    }
}