package edu.northeastern.guildly.signUpFragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;
import edu.northeastern.guildly.MainActivity;

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
            // Now we validate each step individually
            switch (currentStep) {
                case 1: // Profile Info
                    if (profileInfoFragment.validateAndSaveData(signUpData)) {
                        checkUsernameUniqueness();
                    }
                    break;
                case 2: // Habit Selection
                    if (habitSelectionFragment.validateAndSaveData(signUpData)) {
                        currentStep++;
                        navigateToStep(currentStep);
                        updateStepIndicator();
                    }
                    break;
                case 3: // Avatar Selection
                    if (avatarSelectionFragment.validateAndSaveData(signUpData)) {
                        currentStep++;
                        signUpReviewFragment.setArguments(signUpData);
                        navigateToStep(currentStep);
                        updateStepIndicator();
                    }
                    break;
                case 4: // Sign Up Review - Final Step
                    completeSignUp();
                    break;
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

    private void checkUsernameUniqueness() {
        String username = signUpData.getString("username");
        DatabaseReference usernameRef = FirebaseDatabase.getInstance().getReference("usernames").child(username);

        usernameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(MultiStepSignUpActivity.this, "Username already taken.", Toast.LENGTH_SHORT).show();
                } else {
                    // Username is available, advance to next step
                    currentStep++;
                    navigateToStep(currentStep);
                    updateStepIndicator();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MultiStepSignUpActivity.this, "Error checking username.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToStep(int step) {
        switch (step) {
            case 1:
                loadFragment(profileInfoFragment);
                break;
            case 2:
                // Pass the previously collected data to the habit selection fragment
                habitSelectionFragment.setArguments(signUpData);
                loadFragment(habitSelectionFragment);
                break;
            case 3:
                // Pass data to avatar selection fragment
                avatarSelectionFragment.setArguments(signUpData);
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

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
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

    private void completeSignUp() {
        String username = signUpData.getString("username");
        String email = signUpData.getString("email");
        String password = signUpData.getString("password");
        String aboutMe = signUpData.getString("aboutMe", "");
        String profileImageUri = signUpData.getString("profileImageUri", "");
        String userId = signUpData.getString("userId");

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    List<String> newFriends = new ArrayList<>();
                    Map<String, String> newFriendRequests = new HashMap<>();
                    Map<String, Boolean> newChats = new HashMap<>();

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", username);
                    userMap.put("email", email);
                    userMap.put("password", password); // Note: Storing password in plain text is not secure
                    userMap.put("profilePicUrl", profileImageUri);
                    userMap.put("aboutMe", aboutMe);
                    userMap.put("friends", newFriends);
                    userMap.put("friendRequests", newFriendRequests);
                    userMap.put("chats", newChats);

                    String sanitizedEmailKey = email.replace(".", ",");
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(sanitizedEmailKey);
                    DatabaseReference usernameRef = FirebaseDatabase.getInstance().getReference("usernames").child(username);

                    userRef.updateChildren(userMap)
                            .addOnSuccessListener(aVoid -> {
                                usernameRef.setValue(sanitizedEmailKey);
                                MainActivity.currentUserEmail = email;
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Saving user data failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}