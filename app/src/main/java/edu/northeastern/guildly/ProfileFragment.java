package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView profileUsername;
    private TextView streakDescription;
    private ImageView profileEditButton;
    private TextView habitsViewMore;
    private TextView friendsViewMore;

    public ProfileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        setUserData();

        setupClickListeners();
    }

    private void initViews(View view) {

        profileUsername = view.findViewById(R.id.profile_username);
        profileEditButton = view.findViewById(R.id.profile_edit_button);


        streakDescription = view.findViewById(R.id.streak_description);


        habitsViewMore = view.findViewById(R.id.habits_view_more);


        friendsViewMore = view.findViewById(R.id.friends_view_more);
    }

    private void setUserData() {

        profileUsername.setText("Yunmu57");

        streakDescription.setText("You have drank 64oz Water for 31 days straight!!!");

        // TODO: Update with Firebase retrieval logic


        updateHabitImages();
    }

    private void setupClickListeners() {
        // When "View More" in habits section is clicked
        habitsViewMore.setOnClickListener(v -> {
            // Show dialog instead of launching an activity
            showHabitsDialog();
        });

        // Other click listeners can be added here as needed
    }

    private void showHabitsDialog() {
        // Get the list of habits similar to HomeFragment
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

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_view_habits, null);
        ListView listView = dialogView.findViewById(R.id.habit_list_view);

        HabitChoiceAdapter adapter = new HabitChoiceAdapter(getContext(), userHabits);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("My Habits")
                .setView(dialogView)
                .create();

        dialog.show();
    }

    private void updateHabitImages() {

        ImageView habitWorkout = getView().findViewById(R.id.habit_workout);
        ImageView habitMeditation = getView().findViewById(R.id.habit_meditation);
        ImageView habitSwimming = getView().findViewById(R.id.habit_swimming);
        habitWorkout.setImageResource(R.drawable.ic_workout);
        habitMeditation.setImageResource(R.drawable.ic_meditation);
        habitSwimming.setImageResource(R.drawable.ic_water);
    }
}