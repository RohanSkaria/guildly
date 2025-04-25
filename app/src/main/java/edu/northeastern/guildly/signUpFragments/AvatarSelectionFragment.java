package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;

public class AvatarSelectionFragment extends Fragment {
    private CircleImageView avatarGamer, avatarMan, avatarGirl;
    private String selectedAvatar = null;
    private TextView titleText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_avatar_selection, container, false);

        // Initialize views
        titleText = view.findViewById(R.id.tv_avatar_title);
        avatarGamer = view.findViewById(R.id.avatar_gamer);
        avatarMan = view.findViewById(R.id.avatar_man);
        avatarGirl = view.findViewById(R.id.avatar_girl);

        // Restore previously selected avatar if returning to this fragment
        if (getArguments() != null) {
            selectedAvatar = getArguments().getString("profileImageUri");
            updateSelectedAvatarUI(selectedAvatar);
        }

        // Set up click listeners
        avatarGamer.setOnClickListener(v -> selectAvatar("gamer"));
        avatarMan.setOnClickListener(v -> selectAvatar("man"));
        avatarGirl.setOnClickListener(v -> selectAvatar("girl"));

        return view;
    }

    private void selectAvatar(String avatarType) {
        selectedAvatar = avatarType;
        updateSelectedAvatarUI(avatarType);

        // Show a confirmation toast
        String message = "Selected avatar: ";
        switch (avatarType) {
            case "gamer":
                message += "Gamer";
                break;
            case "man":
                message += "Man";
                break;
            case "girl":
                message += "Girl";
                break;
        }
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateSelectedAvatarUI(String avatarType) {
        // Reset all borders
        resetAvatarBorders();

        if (avatarType == null) {
            titleText.setText("Choose Your Avatar");
            return;
        }

        // Highlight selected avatar by increasing border width
        switch (avatarType) {
            case "gamer":
                avatarGamer.setBorderWidth(5);
                titleText.setText("Gamer Avatar Selected");
                break;
            case "man":
                avatarMan.setBorderWidth(5);
                titleText.setText("Man Avatar Selected");
                break;
            case "girl":
                avatarGirl.setBorderWidth(5);
                titleText.setText("Girl Avatar Selected");
                break;
        }
    }

    private void resetAvatarBorders() {
        avatarGamer.setBorderWidth(2);
        avatarMan.setBorderWidth(2);
        avatarGirl.setBorderWidth(2);
    }

    public boolean validateAndSaveData(Bundle data) {
        if (selectedAvatar != null) {
            data.putString("profileImageUri", selectedAvatar);
            return true;
        } else {
            Toast.makeText(getContext(), "Please select an avatar", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}