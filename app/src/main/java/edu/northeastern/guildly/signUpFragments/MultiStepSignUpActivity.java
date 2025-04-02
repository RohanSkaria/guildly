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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

/**
 * Multi-step sign-up flow, storing user info (username, email, etc.).
 * The HabitSelectionFragment writes each habit to /users/<userId>/habits with isTracked.
 */
public class MultiStepSignUpActivity extends AppCompatActivity {

    private TextView tvStepIndicator;
    private Button btnNext, btnBack;

    private int currentStep = 1;
    private final int TOTAL_STEPS = 4;

    private Bundle signUpData = new Bundle();

    private ProfileInfoFragment profileInfoFragment;
    private HabitSelectionFragment habitSelectionFragment;
    private AvatarSelectionFragment avatarSelectionFragment;
    private SignUpReviewFragment signUpReviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_step_sign_up);

        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        profileInfoFragment = new ProfileInfoFragment();
        habitSelectionFragment = new HabitSelectionFragment();
        avatarSelectionFragment = new AvatarSelectionFragment();
        signUpReviewFragment = new SignUpReviewFragment();

        loadFragment(profileInfoFragment);
        updateStepIndicator();

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
                finish();
            }
        });
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
                signUpReviewFragment.setArguments(signUpData);
                loadFragment(signUpReviewFragment);
                btnNext.setText("Create Account");
                break;
        }
        btnBack.setVisibility(View.VISIBLE);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void updateStepIndicator() {
        tvStepIndicator.setText("Step " + currentStep + " of " + TOTAL_STEPS);
        if (currentStep == TOTAL_STEPS) {
            btnNext.setText("Create Account");
        } else {
            btnNext.setText("Next");
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                return profileInfoFragment.validateAndSaveData(signUpData);
            case 2:
                return habitSelectionFragment.validateAndSaveData(signUpData);
            case 3:
                return avatarSelectionFragment.validateAndSaveData(signUpData);
            case 4:
                return true;
        }
        return false;
    }

    private void completeSignUp() {
        String username = signUpData.getString("username");
        String email = signUpData.getString("email");
        String password = signUpData.getString("password");
        String aboutMe = signUpData.getString("aboutMe", "");
        String profileImageUri = signUpData.getString("profileImageUri", "");
        // Retrieve the sanitized userId from the data
        String userId = signUpData.getString("userId");

        List<String> newFriends = new ArrayList<>();
        Map<String, String> newFriendRequests = new HashMap<>();
        Map<String, Boolean> newChats = new HashMap<>();

        // Construct a User object but do NOT include any habit list
        User user = new User(
                username,
                email,
                password,
                profileImageUri,
                aboutMe,
                newFriends,
                newFriendRequests,
                newChats
        );

        // Instead of setValue(user), we'll use updateChildren to avoid overwriting "habits"
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("password", password);
        userMap.put("profilePicUrl", profileImageUri);
        userMap.put("aboutMe", aboutMe);
        // friends, friendRequests, chats if you want them at top-level:
        userMap.put("friends", newFriends);
        userMap.put("friendRequests", newFriendRequests);
        userMap.put("chats", newChats);

        userRef.updateChildren(userMap)
                .addOnSuccessListener(aVoid -> {
                    MainActivity.currentUserEmail = email;
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
