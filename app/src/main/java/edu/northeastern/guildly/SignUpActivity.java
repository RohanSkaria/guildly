package edu.northeastern.guildly;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.data.User;

public class SignUpActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private EditText editTextUsername, editTextAboutMe, editTextEmail, editTextPassword, editTextConfirmPassword;
    private ImageView imageViewProfilePic;
    private Button buttonSelectPic, buttonSignUp, buttonBackToLogin;

    private CheckBox checkBoxDrinkWater, checkBoxWorkout, checkBoxHomework,
            checkBoxReading, checkBoxMeditating, checkBoxSavingMoney,
            checkBoxEatingHealthy, checkBoxNoPhoneAfter10;

    // We'll store the selected image URI as a string (optional)
    private String selectedImageUriString = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextAboutMe = findViewById(R.id.editTextAboutMe);
        editTextEmail = findViewById(R.id.editTextEmailSignUp);
        editTextPassword = findViewById(R.id.editTextPasswordSignUp);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPasswordSignUp);

        imageViewProfilePic = findViewById(R.id.imageViewProfilePic);
        buttonSelectPic = findViewById(R.id.buttonSelectPic);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        checkBoxDrinkWater = findViewById(R.id.checkBoxDrinkWater);
        checkBoxWorkout = findViewById(R.id.checkBoxWorkout);
        checkBoxHomework = findViewById(R.id.checkBoxHomework);
        checkBoxReading = findViewById(R.id.checkBoxReading);
        checkBoxMeditating = findViewById(R.id.checkBoxMeditating);
        checkBoxSavingMoney = findViewById(R.id.checkBoxSavingMoney);
        checkBoxEatingHealthy = findViewById(R.id.checkBoxEatingHealthy);
        checkBoxNoPhoneAfter10 = findViewById(R.id.checkBoxNoPhoneAfter10);

        // Pick an image from gallery (optional)
        buttonSelectPic.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(
                    Intent.createChooser(intent, "Select Profile Picture"),
                    PICK_IMAGE_REQUEST
            );
        });

        // --- SIGN UP LOGIC ---
        buttonSignUp.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String aboutMe = editTextAboutMe.getText().toString().trim();  // optional
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            // Required field checks
            if (TextUtils.isEmpty(username)) {
                editTextUsername.setError("Username is required");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Password is required");
                return;
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                editTextConfirmPassword.setError("Please confirm your password");
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignUpActivity.this,
                        "Passwords do not match!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Gather selected habits (optional; can be empty)
            List<String> selectedHabits = new ArrayList<>();
            if (checkBoxDrinkWater.isChecked())    selectedHabits.add("Drink Water");
            if (checkBoxWorkout.isChecked())       selectedHabits.add("Workout");
            if (checkBoxHomework.isChecked())      selectedHabits.add("Homework");
            if (checkBoxReading.isChecked())       selectedHabits.add("Reading");
            if (checkBoxMeditating.isChecked())    selectedHabits.add("Meditating");
            if (checkBoxSavingMoney.isChecked())   selectedHabits.add("Saving Money");
            if (checkBoxEatingHealthy.isChecked()) selectedHabits.add("Eating Healthy");
            if (checkBoxNoPhoneAfter10.isChecked())selectedHabits.add("No Phone After 10");

            // Initialize empty lists/maps for new user
            List<String> newFriends = new ArrayList<>();               // no friends initially
            Map<String, String> newFriendRequests = new HashMap<>();   // no requests initially
            Map<String, Boolean> newChats = new HashMap<>();           // no chats initially

            // Setup Firebase reference
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference usersRef = database.getReference("users");

            // Replace '.' with ',' to use email as the DB key
            String sanitizedEmailKey = email.replace(".", ",");

            // Build a new User object
            User user = new User(
                    username,
                    email,
                    password,
                    selectedImageUriString,  // profilePicUrl
                    aboutMe,
                    selectedHabits,
                    newFriends,
                    newFriendRequests,
                    newChats   // The new field for 'chats'
            );

            // Save to DB
            usersRef.child(sanitizedEmailKey).setValue(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SignUpActivity.this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT).show();
                        finish(); // close SignUp screen and go back (to Login)
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SignUp", "Failed to register user", e);
                        Toast.makeText(SignUpActivity.this,
                                "Registration failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        // --- BACK TO LOGIN ---
        buttonBackToLogin.setOnClickListener(view -> {
            // If we came from LoginActivity, just finish.
            finish();

            // OR explicitly:
            // Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            // startActivity(intent);
            // finish();
        });
    }

    // Handle profile image picking
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedImageUriString = imageUri.toString();
            imageViewProfilePic.setImageURI(imageUri);
        }
    }
}
