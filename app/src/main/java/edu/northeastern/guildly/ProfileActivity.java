package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Arrays;
import java.util.List;


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
            showHabitsDialog();
        });
    }

    private void showHabitsDialog() {

        List<Habit> userHabits = Arrays.asList(
                new Habit("Drink 64oz of water", R.drawable.ic_water),
                new Habit("Workout for 30 mins", R.drawable.ic_workout),
                new Habit("Do homework", R.drawable.ic_homework),
                new Habit("Read a book", R.drawable.ic_book),
                new Habit("Meditate for 10 minutes", R.drawable.ic_meditation),
                new Habit("Save money today", R.drawable.ic_savemoney),
                new Habit("Eat vegetables", R.drawable.ic_vegetable),
                new Habit("No phone after 10PM", R.drawable.ic_phonebanned)
        );

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_habits, null);
        ListView listView = dialogView.findViewById(R.id.habit_list_view);

        HabitChoiceAdapter adapter = new HabitChoiceAdapter(this, userHabits);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("My Habits")
                .setView(dialogView)
                .create();

        dialog.show();
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