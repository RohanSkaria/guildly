package edu.northeastern.guildly;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.northeastern.guildly.data.User;
import edu.northeastern.guildly.signUpFragments.MultiStepSignUpActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView buttonGoToSignUp, textViewForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean fromLogout = getIntent().getBooleanExtra("logout", false);
        SharedPreferences prefs = getSharedPreferences("GuildlyPrefs", MODE_PRIVATE);
        String savedEmail = prefs.getString("loggedInUserEmail", null);
        if (savedEmail != null && !fromLogout) {
            MainActivity.currentUserEmail = savedEmail;
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmailLogin);
        editTextPassword = findViewById(R.id.editTextPasswordLogin);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoToSignUp = findViewById(R.id.buttonGoToSignUp);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Password is required");
                return;
            }

            String sanitizedEmailKey = email.replace(".", ",");
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference usersRef = database.getReference("users");

            usersRef.child(sanitizedEmailKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.password.equals(password)) {
                            SharedPreferences prefs = getSharedPreferences("GuildlyPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("loggedInUserEmail", email);
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            MainActivity.currentUserEmail = email;
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid password. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User not found. Please sign up first.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(LoginActivity.this, "Login failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        buttonGoToSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, MultiStepSignUpActivity.class);
            startActivity(intent);
        });

        textViewForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }
}