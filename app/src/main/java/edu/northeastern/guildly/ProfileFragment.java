package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.SettingsActivity;
import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.adapters.HabitAdapter;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.User;

public class ProfileFragment extends Fragment {

    private EditText profileUsername;
    private TextView streakDescription;
    private ImageView profileEditButton;
    private TextView habitsViewMore;
    private TextView friendsViewMore;
    private DatabaseReference userRef;
    private String myUserKey;
    private TextView profileAboutMe;
    private ImageView aboutMeEditButton;
    private ImageView settingsButton;
    private CircleImageView profileImage;

    public ProfileFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Figure out which user we're displaying
        String myEmail = MainActivity.currentUserEmail;
        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER";
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(myUserKey);

        initViews(view);
        loadUserDataFromFirebase();
        setupClickListeners();
    }

    private void initViews(View view) {
        profileUsername    = view.findViewById(R.id.profile_username);
        profileEditButton  = view.findViewById(R.id.profile_edit_button);
        streakDescription  = view.findViewById(R.id.streak_description);
        habitsViewMore     = view.findViewById(R.id.habits_view_more);
        friendsViewMore    = view.findViewById(R.id.friends_view_more);
        profileAboutMe     = view.findViewById(R.id.profile_about_me);
        aboutMeEditButton  = view.findViewById(R.id.about_me_edit_button);
        settingsButton     = view.findViewById(R.id.settings_button);
        profileImage       = view.findViewById(R.id.profile_image);
    }

    /**
     * Pulls user data from DB, sets the username, aboutMe,
     * any "streak" info, plus sets the avatar if stored in "profilePicUrl".
     */
    private void loadUserDataFromFirebase() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    // Username
                    profileUsername.setText(
                            TextUtils.isEmpty(user.username)
                                    ? "UnnamedUser"
                                    : user.username
                    );

                    // AboutMe
                    if (!TextUtils.isEmpty(user.aboutMe)) {
                        profileAboutMe.setText(user.aboutMe);
                    } else {
                        profileAboutMe.setText("Add a bio...");
                    }

                    // If you store the avatar name (like "man", "gamer", etc.)
                    if (!TextUtils.isEmpty(user.profilePicUrl)) {
                        updateProfileAvatar(user.profilePicUrl);
                    }

                    // If you want to display a "streak" or something,
                    // you can set it from user data. This is placeholder:
                    // streakDescription might say "Longest streak: 10 days" or so
                    streakDescription.setText("No global streak data yet...");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load user data: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        habitsViewMore.setOnClickListener(v -> showHabitsDialog());
        friendsViewMore.setOnClickListener(v -> showFriendsDialog());

        profileEditButton.setOnClickListener(v -> toggleUsernameEditing());
        profileImage.setOnClickListener(v -> showSelectAvatarDialog());

        View aboutMeCard = getView().findViewById(R.id.about_me_card);
        aboutMeCard.setOnClickListener(v -> showEditAboutMeDialog());
        aboutMeEditButton.setOnClickListener(v -> showEditAboutMeDialog());

        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
    }

    /**
     * Let user edit 'aboutMe' in a dialog, update in DB.
     */
    private void showEditAboutMeDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_about_me, null);
        EditText editAboutMe = dialogView.findViewById(R.id.edit_about_me);

        String currentAboutMe = profileAboutMe.getText().toString();
        if (!currentAboutMe.equals("Add a bio...")) {
            editAboutMe.setText(currentAboutMe);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String newAboutMe = editAboutMe.getText().toString().trim();
                    if (!newAboutMe.isEmpty()) {
                        profileAboutMe.setText(newAboutMe);
                        userRef.child("aboutMe").setValue(newAboutMe)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(getContext(),
                                                "Bio updated", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to update bio",
                                                Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    /**
     * Toggle editing of the username field.
     * If saving => update the DB.
     */
    private void toggleUsernameEditing() {
        if (!profileUsername.isEnabled()) {
            // Enter "edit mode"
            profileUsername.setEnabled(true);
            profileUsername.setFocusableInTouchMode(true);
            profileUsername.requestFocus();
            profileUsername.setSelection(
                    profileUsername.getText().length()
            );
            profileEditButton.setImageResource(android.R.drawable.ic_menu_save);

        } else {
            // Exit "edit mode" => save to DB
            String newUsername = profileUsername.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                userRef.child("username").setValue(newUsername)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(),
                                    "Username updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(),
                                    "Failed to update username", Toast.LENGTH_SHORT).show();
                        });
            }

            profileUsername.setEnabled(false);
            profileEditButton.setImageResource(R.drawable.ic_edit);
        }
    }

    /**
     * Show a dialog with a RecyclerView of just the user’s tracked habits (isTracked=true),
     * using your HabitAdapter in 'home mode' (isSelectionMode=false).
     */
    private void showHabitsDialog() {
        // 1) Load all habits from /users/<myUserKey>/habits, filter for isTracked = true
        userRef.child("habits").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Habit> trackedHabits = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Habit h = ds.getValue(Habit.class);
                    if (h != null && h.isTracked()) {
                        trackedHabits.add(h);
                    }
                }

                // 2) Now inflate a dialog with a RecyclerView
                View dialogView = LayoutInflater.from(getContext())
                        .inflate(R.layout.dialog_add_habit, null);
                // In your XML, we might rename "habit_list_view" to a RecyclerView
                // So let's assume we have a RecyclerView with id=habit_list_recycler:
                RecyclerView rv = dialogView.findViewById(R.id.habit_list_view);
                rv.setLayoutManager(new LinearLayoutManager(getContext()));

                // 3) Create the adapter (isSelectionMode=false => daily completion logic)
                HabitAdapter adapter = new HabitAdapter(
                        trackedHabits,
                        userRef.child("habits"),
                        false
                );
                rv.setAdapter(adapter);

                // 4) Show dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("My Habits")
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .create()
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load habits: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Show a placeholder "My Friends" dialog or
     * pull from DB if you store friends in /users/<uid>/friends
     */
    private void showFriendsDialog() {
        // For now, just show a placeholder
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_view_friends, null);

        // You might have a RecyclerView or a ListView in dialog_view_friends
        ListView listView = dialogView.findViewById(R.id.friends_list_view);

        // TODO: Actually load from DB if you want
        // For now, no code that references FriendChoiceAdapter

        new AlertDialog.Builder(requireContext())
                .setTitle("My Friends")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()
                .show();
    }

    /**
     * Let user select one of the 3 avatars. Save that choice in "profilePicUrl."
     */
    private void showSelectAvatarDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_select_avatar, null);

        CircleImageView avatarGamer = dialogView.findViewById(R.id.avatar_gamer);
        CircleImageView avatarMan   = dialogView.findViewById(R.id.avatar_man);
        CircleImageView avatarGirl  = dialogView.findViewById(R.id.avatar_girl);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Select Avatar")
                .setView(dialogView)
                .create();

        avatarGamer.setOnClickListener(v -> {
            updateProfileAvatar("gamer");
            dialog.dismiss();
        });
        avatarMan.setOnClickListener(v -> {
            updateProfileAvatar("man");
            dialog.dismiss();
        });
        avatarGirl.setOnClickListener(v -> {
            updateProfileAvatar("girl");
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Sets local image resource & writes "profilePicUrl" to DB.
     */
    private void updateProfileAvatar(String avatarName) {
        int resourceId;
        switch (avatarName) {
            case "gamer":
                resourceId = R.drawable.gamer;
                break;
            case "man":
                resourceId = R.drawable.man;
                break;
            case "girl":
                resourceId = R.drawable.girl;
                break;
            default:
                resourceId = R.drawable.unknown_profile;
                break;
        }
        profileImage.setImageResource(resourceId);

        userRef.child("profilePicUrl").setValue(avatarName)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(),
                                "Avatar updated", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to update avatar", Toast.LENGTH_SHORT).show()
                );
    }
}
