package edu.northeastern.guildly;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.northeastern.guildly.model.User; // Import your User model

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonGoToSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmailLogin);
        editTextPassword = findViewById(R.id.editTextPasswordLogin);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoToSignUp = findViewById(R.id.buttonGoToSignUp);

        // On "Login" button click
        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // Basic validation
            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Password is required");
                return;
            }

            // Convert email to a valid Firebase Realtime DB key
            String sanitizedEmailKey = email.replace(".", ",");

            // Reference to "users" in your Realtime Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference usersRef = database.getReference("users");

            // Check if user with that email key exists
            usersRef.child(sanitizedEmailKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Convert the snapshot to your User class
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            // Compare passwords
                            if (user.password.equals(password)) {
                                Toast.makeText(LoginActivity.this,
                                        "Login successful!",
                                        Toast.LENGTH_SHORT).show();

                                // Go to MainActivity
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Invalid password. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "User not found. Please sign up first.",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("Login", "Failed to read user data", error.toException());
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });

        // On "Go to Sign Up" button click
        buttonGoToSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}
