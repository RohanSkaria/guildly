package edu.northeastern.guildly;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileUsername;
    private TextView streakDescription;
    private ImageView profileEditButton; // buttons - static for now
    private TextView habitsViewMore;
    private TextView friendsViewMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize views
        initViews();

        // Set data from firebase
        setUserData();
    }

    private void initViews() {
        // Profile section
        profileUsername = findViewById(R.id.profile_username);
        profileEditButton = findViewById(R.id.profile_edit_button);

        // Streak section
        streakDescription = findViewById(R.id.streak_description);

        // Habits section
        habitsViewMore = findViewById(R.id.habits_view_more);

        // Friends section
        friendsViewMore = findViewById(R.id.friends_view_more);

         // set click listeners when implemented
    }

    private void setUserData() {
        // Set profile data
        profileUsername.setText("Yunmu57");

        // Set streak data
        streakDescription.setText("You have drank 64oz Water for 31 days straight!!!");

        // need to base the profile pictures and other data based on firebase information
    }
}