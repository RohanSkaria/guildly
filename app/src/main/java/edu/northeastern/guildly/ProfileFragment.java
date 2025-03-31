package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

        habitsViewMore.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewHabitsActivity.class);
            startActivity(intent);
        });


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