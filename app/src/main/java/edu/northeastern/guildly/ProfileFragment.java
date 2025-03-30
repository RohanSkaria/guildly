package edu.northeastern.guildly;

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
    private ImageView profileEditButton; // Buttons - static for now
    private TextView habitsViewMore;
    private TextView friendsViewMore;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initViews(view);

        // Set user data (replace with Firebase retrieval logic if necessary)
        setUserData();
    }

    private void initViews(View view) {
        // Profile section
        profileUsername = view.findViewById(R.id.profile_username);
        profileEditButton = view.findViewById(R.id.profile_edit_button);

        // Streak section
        streakDescription = view.findViewById(R.id.streak_description);

        // Habits section
        habitsViewMore = view.findViewById(R.id.habits_view_more);

        // Friends section
        friendsViewMore = view.findViewById(R.id.friends_view_more);

        // TODO: Set click listeners if needed
    }

    private void setUserData() {
        // Set profile data
        profileUsername.setText("Yunmu57");

        // Set streak data
        streakDescription.setText("You have drank 64oz Water for 31 days straight!!!");

        // TODO: Replace with Firebase retrieval logic
    }
}
