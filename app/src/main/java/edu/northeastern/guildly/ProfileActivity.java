package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;



public class ProfileActivity extends AppCompatActivity {

    private TextView profileUsername;
    private TextView streakDescription;
    private ImageView profileEditButton;
    private TextView habitsViewMore;
    private TextView friendsViewMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        initViews();


        setUserData();


        setupClickListeners();
    }

    private void initViews() {

        profileUsername = findViewById(R.id.profile_username);
        profileEditButton = findViewById(R.id.profile_edit_button);
        streakDescription = findViewById(R.id.streak_description);
        habitsViewMore = findViewById(R.id.habits_view_more);
        friendsViewMore = findViewById(R.id.friends_view_more);
    }

    private void setUserData() {
        profileUsername.setText("Yunmu57");
        streakDescription.setText("You have drank 64oz Water for 31 days straight!!!");
        updateHabitImages();
    }

    private void setupClickListeners() {
        habitsViewMore.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewHabitsActivity.class);
            startActivity(intent);
        });
    }

    private void updateHabitImages() {
        ImageView habitWorkout = findViewById(R.id.habit_workout);
        ImageView habitMeditation = findViewById(R.id.habit_meditation);
        ImageView habitSwimming = findViewById(R.id.habit_swimming);
        habitWorkout.setImageResource(R.drawable.ic_workout);
        habitMeditation.setImageResource(R.drawable.ic_meditation);
        habitSwimming.setImageResource(R.drawable.ic_water);
    }
}