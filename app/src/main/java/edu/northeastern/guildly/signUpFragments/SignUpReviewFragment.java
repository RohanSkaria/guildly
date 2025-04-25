package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;

public class SignUpReviewFragment extends Fragment {
    private TextView tvUsername, tvEmail, tvAboutMe, tvHabits;
    private CircleImageView ivAvatar;

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

            String aboutMe = args.getString("aboutMe", "");
            if (!aboutMe.isEmpty()) {
                tvAboutMe.setText(aboutMe);
            } else {
                tvAboutMe.setText(R.string.no_about_me_provided);
            }

            // Format habits as a list
            ArrayList<String> habits = args.getStringArrayList("selectedHabits");
            if (habits != null && !habits.isEmpty()) {
                StringBuilder habitsText = new StringBuilder();
                for (String habit : habits) {
                    habitsText.append("• ").append(habit).append("\n");
                }
                tvHabits.setText(habitsText.toString());
            } else {
                tvHabits.setText(R.string.no_habits_selected);
            }

            // Set avatar based on the profileImageUri string
            String avatarType = args.getString("profileImageUri");
            if (avatarType != null) {
                switch (avatarType) {
                    case "gamer":
                        ivAvatar.setImageResource(R.drawable.gamer);
                        ivAvatar.setCircleBackgroundColor(getResources().getColor(R.color.gamer_bg));
                        break;
                    case "man":
                        ivAvatar.setImageResource(R.drawable.man);
                        ivAvatar.setCircleBackgroundColor(getResources().getColor(R.color.man_bg));
                        break;
                    case "girl":
                        ivAvatar.setImageResource(R.drawable.girl);
                        ivAvatar.setCircleBackgroundColor(getResources().getColor(R.color.girl_bg));
                        break;
                    default:
                        ivAvatar.setImageResource(R.drawable.unknown_profile);
                        break;
                }
            }
        }

        return view;
    }
}