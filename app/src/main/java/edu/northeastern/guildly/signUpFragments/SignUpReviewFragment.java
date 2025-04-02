package edu.northeastern.guildly.signUpFragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import edu.northeastern.guildly.R;

public class SignUpReviewFragment extends Fragment {
    private TextView tvUsername, tvEmail, tvAboutMe, tvHabits;
    private ImageView ivAvatar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up_review, container, false);

        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAboutMe = view.findViewById(R.id.tvAboutMe);
        tvHabits = view.findViewById(R.id.tvHabits);
        ivAvatar = view.findViewById(R.id.ivAvatar);

        // Display collected data
        Bundle args = getArguments();
        if (args != null) {
            tvUsername.setText(args.getString("username", ""));
            tvEmail.setText(args.getString("email", ""));
            tvAboutMe.setText(args.getString("aboutMe", ""));

            // Format habits as a list
            ArrayList<String> habits = args.getStringArrayList("selectedHabits");
            if (habits != null && !habits.isEmpty()) {
                StringBuilder habitsText = new StringBuilder();
                for (String habit : habits) {
                    habitsText.append("• ").append(habit).append("\n");
                }
                tvHabits.setText(habitsText.toString());
            } else {
                tvHabits.setText("No habits selected");
            }

            // Set avatar if available
            String imageUri = args.getString("profileImageUri");
            if (imageUri != null && !imageUri.isEmpty()) {
                ivAvatar.setImageURI(Uri.parse(imageUri));
            }
        }

        return view;
    }
}
