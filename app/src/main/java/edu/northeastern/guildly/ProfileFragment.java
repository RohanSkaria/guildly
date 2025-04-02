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
import java.util.Collections;
import java.util.Comparator;
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

    // Top-3 habit icon slots
    private ImageView ivTopHabit1, ivTopHabit2, ivTopHabit3;
    private TextView tvTopHabit1, tvTopHabit2, tvTopHabit3;
    private TextView tvNoHabitsMessage; // if we have 0 total tracked habits

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

        // Determine user
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
        profileUsername   = view.findViewById(R.id.profile_username);
        profileEditButton = view.findViewById(R.id.profile_edit_button);
        streakDescription = view.findViewById(R.id.streak_description);
        habitsViewMore    = view.findViewById(R.id.habits_view_more);
        friendsViewMore   = view.findViewById(R.id.friends_view_more);
        profileAboutMe    = view.findViewById(R.id.profile_about_me);
        aboutMeEditButton = view.findViewById(R.id.about_me_edit_button);
        settingsButton    = view.findViewById(R.id.settings_button);
        profileImage      = view.findViewById(R.id.profile_image);

        // Top 3 habit slots
        ivTopHabit1 = view.findViewById(R.id.ivTopHabit1);
        ivTopHabit2 = view.findViewById(R.id.ivTopHabit2);
        ivTopHabit3 = view.findViewById(R.id.ivTopHabit3);

        tvTopHabit1 = view.findViewById(R.id.tvTopHabit1);
        tvTopHabit2 = view.findViewById(R.id.tvTopHabit2);
        tvTopHabit3 = view.findViewById(R.id.tvTopHabit3);

        tvNoHabitsMessage = view.findViewById(R.id.tvNoHabitsMessage);
    }

    /**
     * Pulls user data from DB, sets the username, aboutMe, avatar, etc.
     * Then loads tracked habits to find top 3 by streak.
     */
    private void loadUserDataFromFirebase() {
        // 1) Load user top-level fields
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

                    // Avatar
                    if (!TextUtils.isEmpty(user.profilePicUrl)) {
                        updateProfileAvatar(user.profilePicUrl);
                    }
                }

                // 2) Then load tracked habits => do longest streak + show top 3
                loadTrackedHabitsAndSort();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Failed to load user data: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Load only habits where isTracked=true => find best streak => fill top 3 icons
     */
    private void loadTrackedHabitsAndSort() {
        userRef.child("habits").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Habit> tracked = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Habit h = ds.getValue(Habit.class);
                    if (h != null && h.isTracked()) {
                        tracked.add(h);
                    }
                }

                // If none => tvNoHabitsMessage => "No current habits!"
                if (tracked.isEmpty()) {
                    tvNoHabitsMessage.setVisibility(View.VISIBLE);
                    // Hide the icon slots
                    ivTopHabit1.setVisibility(View.GONE);
                    ivTopHabit2.setVisibility(View.GONE);
                    ivTopHabit3.setVisibility(View.GONE);
                    tvTopHabit1.setVisibility(View.GONE);
                    tvTopHabit2.setVisibility(View.GONE);
                    tvTopHabit3.setVisibility(View.GONE);

                    // Also set "No streak yet!" or something
                    streakDescription.setText("No streak yet!");
                    return;
                } else {
                    tvNoHabitsMessage.setVisibility(View.GONE);
                }

                // Sort by streak desc
                Collections.sort(tracked, new Comparator<Habit>() {
                    @Override
                    public int compare(Habit o1, Habit o2) {
                        return o2.getStreakCount() - o1.getStreakCount();
                    }
                });

                // The first element => best streak
                Habit top = tracked.get(0);
                if (top.getStreakCount() > 0) {
                    streakDescription.setText("Longest streak: " +
                            top.getStreakCount() + " days of " +
                            top.getHabitName() + "!");
                } else {
                    streakDescription.setText("No streak yet!");
                }

                // Now fill up to 3
                setTopHabitSlot(0, tracked);
                setTopHabitSlot(1, tracked);
                setTopHabitSlot(2, tracked);
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
     * Set the i-th slot with the i-th Habit in sorted list if i < size,
     * else hide it.
     */
    private void setTopHabitSlot(int i, List<Habit> sorted) {
        ImageView iv;
        TextView tv;
        if (i == 0) {
            iv = ivTopHabit1; tv = tvTopHabit1;
        } else if (i == 1) {
            iv = ivTopHabit2; tv = tvTopHabit2;
        } else {
            iv = ivTopHabit3; tv = tvTopHabit3;
        }

        if (i >= sorted.size()) {
            // Hide
            iv.setVisibility(View.GONE);
            tv.setVisibility(View.GONE);
        } else {
            // Show
            iv.setVisibility(View.VISIBLE);
            tv.setVisibility(View.VISIBLE);

            Habit h = sorted.get(i);
            iv.setImageResource(h.getIconResId());
            tv.setText("🔥 " + h.getStreakCount() + " days");
        }
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

    // Let user edit 'aboutMe'
    private void showEditAboutMeDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_about_me, null);
        EditText editAboutMe = dialogView.findViewById(R.id.edit_about_me);

        String currentAboutMe = profileAboutMe.getText().toString();
        if (!currentAboutMe.equals("Add a bio...")) {
            editAboutMe.setText(currentAboutMe);
        }

        new AlertDialog.Builder(requireContext())
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
                .create()
                .show();
    }

    // Let user toggle username editing
    private void toggleUsernameEditing() {
        if (!profileUsername.isEnabled()) {
            profileUsername.setEnabled(true);
            profileUsername.setFocusableInTouchMode(true);
            profileUsername.requestFocus();
            profileUsername.setSelection(profileUsername.getText().length());
            profileEditButton.setImageResource(android.R.drawable.ic_menu_save);
        } else {
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

    // Show only tracked habits in a dialog
    private void showHabitsDialog() {
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

                View dialogView = LayoutInflater.from(getContext())
                        .inflate(R.layout.dialog_add_habit, null);

                RecyclerView rv = dialogView.findViewById(R.id.habit_list_view);
                rv.setLayoutManager(new LinearLayoutManager(getContext()));

                HabitAdapter adapter = new HabitAdapter(
                        trackedHabits,
                        userRef.child("habits"),
                        false
                );
                rv.setAdapter(adapter);

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

    // For friends
    private void showFriendsDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_view_friends, null);

        ListView listView = dialogView.findViewById(R.id.friends_list_view);
        // TODO: load from DB if desired

        new AlertDialog.Builder(requireContext())
                .setTitle("My Friends")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()
                .show();
    }

    // Choose avatar
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
