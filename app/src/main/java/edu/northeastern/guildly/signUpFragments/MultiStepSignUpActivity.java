package edu.northeastern.guildly.signUpFragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

public class MultiStepSignUpActivity extends AppCompatActivity {

    private TextView tvStepIndicator;
    private Button btnNext, btnBack;

    // Step tracking
    private int currentStep = 1;
    private final int TOTAL_STEPS = 4;

    // Data bundle to pass between fragments
    private Bundle signUpData = new Bundle();

    // Fragment references
    private ProfileInfoFragment profileInfoFragment;
    private HabitSelectionFragment habitSelectionFragment;
    private AvatarSelectionFragment avatarSelectionFragment;
    private SignUpReviewFragment signUpReviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_multi_step_sign_up);

            tvStepIndicator = findViewById(R.id.tvStepIndicator);
            btnNext = findViewById(R.id.btnNext);
            btnBack = findViewById(R.id.btnBack);

            // Initialize fragments
            profileInfoFragment = new ProfileInfoFragment();
            habitSelectionFragment = new HabitSelectionFragment();
            avatarSelectionFragment = new AvatarSelectionFragment();
            signUpReviewFragment = new SignUpReviewFragment();

            // Set initial fragment
            loadFragment(profileInfoFragment);
            updateStepIndicator();

            // Set button listeners
            btnNext.setOnClickListener(v -> {
                if (validateCurrentStep()) {
                    if (currentStep < TOTAL_STEPS) {
                        currentStep++;
                        navigateToStep(currentStep);
                    } else {
                        completeSignUp();
                    }
                    updateStepIndicator();
                }
            });

            btnBack.setOnClickListener(v -> {
                if (currentStep > 1) {
                    currentStep--;
                    navigateToStep(currentStep);
                    updateStepIndicator();
                } else {
                    finish(); // Go back to login
                }
            });

        } catch (Exception e) {
            Log.e("SignUpError", "Error in MultiStepSignUpActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToStep(int step) {
        switch (step) {
            case 1:
                loadFragment(profileInfoFragment);
                break;
            case 2:
                loadFragment(habitSelectionFragment);
                break;
            case 3:
                loadFragment(avatarSelectionFragment);
                break;
            case 4:
                // Pass all collected data to review fragment
                signUpReviewFragment.setArguments(signUpData);
                loadFragment(signUpReviewFragment);
                btnNext.setText("Create Account");
                break;
        }

        // Update button visibility
        btnBack.setVisibility(View.VISIBLE);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void updateStepIndicator() {
        tvStepIndicator.setText("Step " + currentStep + " of " + TOTAL_STEPS);

        // Update next button text
        if (currentStep == TOTAL_STEPS) {
            btnNext.setText("Create Account");
        } else {
            btnNext.setText("Next");
        }
    }

    private boolean validateCurrentStep() {
        // Each fragment will implement its own validation
        switch (currentStep) {
            case 1:
                return profileInfoFragment.validateAndSaveData(signUpData);
            case 2:
                return habitSelectionFragment.validateAndSaveData(signUpData);
            case 3:
                return avatarSelectionFragment.validateAndSaveData(signUpData);
            case 4:
                return true; // Review step always valid
        }
        return false;
    }

    private void completeSignUp() {
        // Get final data from bundle
        String username = signUpData.getString("username");
        String email = signUpData.getString("email");
        String password = signUpData.getString("password");
        String aboutMe = signUpData.getString("aboutMe", "");
        ArrayList<String> selectedHabits = signUpData.getStringArrayList("selectedHabits");
        String profileImageUri = signUpData.getString("profileImageUri", "");

        // Initialize empty lists/maps for new user
        List<String> newFriends = new ArrayList<>();
        Map<String, String> newFriendRequests = new HashMap<>();
        Map<String, Boolean> newChats = new HashMap<>();

        // Convert Firebase key
        String sanitizedEmailKey = email.replace(".", ",");

        // Create and save user
        User user = new User(
                username,
                email,
                password,
                profileImageUri,
                aboutMe,
                selectedHabits,
                newFriends,
                newFriendRequests,
                newChats
        );

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(sanitizedEmailKey)
                .setValue(user)
                .addOnSuccessListener(aVoid -> {
                    MainActivity.currentUserEmail = email;
                    // Go to MainActivity
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Show error
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}