package edu.northeastern.guildly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.northeastern.guildly.signUpFragments.MultiStepSignUpActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private ImageView togglePasswordLogin;
    private Button buttonLogin;
    private TextView buttonGoToSignUp, textViewForgotPassword;
    private FirebaseAuth firebaseAuth;
    private boolean isPasswordVisible = false;

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

        firebaseAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.editTextEmailLogin);
        editTextPassword = findViewById(R.id.editTextPasswordLogin);
        togglePasswordLogin = findViewById(R.id.togglePasswordLogin);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoToSignUp = findViewById(R.id.buttonGoToSignUp);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        togglePasswordLogin.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                editTextPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            editTextPassword.setSelection(editTextPassword.getText().length());
        });

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

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String sanitizedEmailKey = email.replace(".", ",");
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(sanitizedEmailKey);

                                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            SharedPreferences.Editor editor = getSharedPreferences("GuildlyPrefs", MODE_PRIVATE).edit();
                                            editor.putString("loggedInUserEmail", email);
                                            editor.apply();

                                            MainActivity.currentUserEmail = email;
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(LoginActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Login failed: Please check your email and password",
                                    Toast.LENGTH_LONG).show();
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