package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
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
import edu.northeastern.guildly.adapters.AllHabitsAdapter;
import edu.northeastern.guildly.adapters.FriendsAdapter;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.User;

/**
 * ProfileFragment that shows:
 *  - username, about me
 *  - top 3 habits by streak
 *  - up to 3 friends (profile pic + username)
 *  - "View More" to see the full friend list
 */
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

    // Top-3 habit slots
    private ImageView ivTopHabit1, ivTopHabit2, ivTopHabit3;
    private TextView tvTopHabit1, tvTopHabit2, tvTopHabit3;
    private TextView tvNoHabitsMessage;

    // Friend slots
    private CircleImageView friendOne, friendTwo, friendThree;
    private TextView friendOneName, friendTwoName, friendThreeName;
    private TextView tvNoFriendsMessage;

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

        // Which user?
        String myEmail = MainActivity.currentUserEmail;
        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER";
        userRef = FirebaseDatabase.getInstance().getReference("users").child(myUserKey);

        initViews(view);
        loadUserDataFromFirebase(); // <-- original single-value approach
        setupClickListeners();

        // ------------------------------------------------------------------------------------------
        // ADD: GuildlyDataManager for REAL-TIME habit updates (without removing existing code)
        // ------------------------------------------------------------------------------------------
        if (!"NO_USER".equals(myUserKey)) {
            GuildlyDataManager manager = GuildlyDataManager.getInstance();
            manager.init(myUserKey);

            // Observe the habitsLiveData. Whenever data changes in Firebase, update top-3 habits:
            manager.getHabitsLiveData().observe(getViewLifecycleOwner(), new Observer<List<Habit>>() {
                @Override
                public void onChanged(List<Habit> updatedHabits) {
                    if (updatedHabits == null) return;

                    // Filter only tracked habits
                    List<Habit> tracked = new ArrayList<>();
                    for (Habit h : updatedHabits) {
                        if (h != null && h.isTracked()) {
                            tracked.add(h);
                        }
                    }
                    // This replicates the logic in loadTrackedHabitsAndSort():
                    if (tracked.isEmpty()) {
                        tvNoHabitsMessage.setVisibility(View.VISIBLE);
                        ivTopHabit1.setVisibility(View.GONE);
                        ivTopHabit2.setVisibility(View.GONE);
                        ivTopHabit3.setVisibility(View.GONE);
                        tvTopHabit1.setVisibility(View.GONE);
                        tvTopHabit2.setVisibility(View.GONE);
                        tvTopHabit3.setVisibility(View.GONE);

                        streakDescription.setText("No streak yet!");
                    } else {
                        tvNoHabitsMessage.setVisibility(View.GONE);

                        // Sort by streak desc
                        Collections.sort(tracked, new Comparator<Habit>() {
                            @Override
                            public int compare(Habit o1, Habit o2) {
                                return o2.getStreakCount() - o1.getStreakCount();
                            }
                        });

                        // The first => best streak
                        Habit top = tracked.get(0);
                        if (top.getStreakCount() > 0) {
                            streakDescription.setText("Longest streak: " +
                                    top.getStreakCount() + " days of " +
                                    top.getHabitName() + "!");
                        } else {
                            streakDescription.setText("No streak yet!");
                        }

                        // Fill top 3
                        setTopHabitSlot(0, tracked);
                        setTopHabitSlot(1, tracked);
                        setTopHabitSlot(2, tracked);
                    }
                }
            });
        }
    }

    private void initViews(View view) {
        // Basic profile fields
        profileUsername   = view.findViewById(R.id.profile_username);
        profileEditButton = view.findViewById(R.id.profile_edit_button);
        streakDescription = view.findViewById(R.id.streak_description);
        habitsViewMore    = view.findViewById(R.id.habits_view_more);
        friendsViewMore   = view.findViewById(R.id.friends_view_more);
        profileAboutMe    = view.findViewById(R.id.profile_about_me);
        aboutMeEditButton = view.findViewById(R.id.about_me_edit_button);
        settingsButton    = view.findViewById(R.id.settings_button);
        profileImage      = view.findViewById(R.id.profile_image);

        // Top habits
        ivTopHabit1 = view.findViewById(R.id.ivTopHabit1);
        ivTopHabit2 = view.findViewById(R.id.ivTopHabit2);
        ivTopHabit3 = view.findViewById(R.id.ivTopHabit3);
        tvTopHabit1 = view.findViewById(R.id.tvTopHabit1);
        tvTopHabit2 = view.findViewById(R.id.tvTopHabit2);
        tvTopHabit3 = view.findViewById(R.id.tvTopHabit3);
        tvNoHabitsMessage = view.findViewById(R.id.tvNoHabitsMessage);

        // Friends top-3
        friendOne   = view.findViewById(R.id.friend_one);
        friendTwo   = view.findViewById(R.id.friend_two);
        friendThree = view.findViewById(R.id.friend_three);

        friendOneName   = view.findViewById(R.id.friend_one_name);
        friendTwoName   = view.findViewById(R.id.friend_two_name);
        friendThreeName = view.findViewById(R.id.friend_three_name);

        // "Add a friend!" if none
        tvNoFriendsMessage = view.findViewById(R.id.tvNoFriendsMessage);
    }

    /**
     * Loads user-level data (username, aboutMe, avatar).
     * Then loads top 3 tracked habits & top 3 friends (single-value).
     */
    private void loadUserDataFromFirebase() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    // Username
                    profileUsername.setText(TextUtils.isEmpty(user.username)
                            ? "UnnamedUser" : user.username);

                    // About me
                    if (!TextUtils.isEmpty(user.aboutMe)) {
                        profileAboutMe.setText(user.aboutMe);
                    } else {
                        profileAboutMe.setText("Add a bio...");
                    }

                    if (!TextUtils.isEmpty(user.profilePicUrl)) {
                        updateProfileAvatar(user.profilePicUrl, false);
                    }

                    // Now load top 3 habits (single-value read)
                    loadTrackedHabitsAndSort();

                    // Now load top 3 friends
                    loadFriendsAndShowTop3(user.friends);
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

    /**
     * Load only tracked habits => find best streak => fill top 3 icons (single-value).
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

                if (tracked.isEmpty()) {
                    tvNoHabitsMessage.setVisibility(View.VISIBLE);
                    // Hide the 3 icon slots
                    ivTopHabit1.setVisibility(View.GONE);
                    ivTopHabit2.setVisibility(View.GONE);
                    ivTopHabit3.setVisibility(View.GONE);
                    tvTopHabit1.setVisibility(View.GONE);
                    tvTopHabit2.setVisibility(View.GONE);
                    tvTopHabit3.setVisibility(View.GONE);

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

                // The first => best streak
                Habit top = tracked.get(0);
                if (top.getStreakCount() > 0) {
                    streakDescription.setText("Longest streak: " +
                            top.getStreakCount() + " days of " +
                            top.getHabitName() + "!");
                } else {
                    streakDescription.setText("No streak yet!");
                }

                // Fill top 3
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

    private void setTopHabitSlot(int i, List<Habit> sorted) {
        ImageView iv;
        TextView tv;
        if (i == 0)      { iv = ivTopHabit1; tv = tvTopHabit1; }
        else if (i == 1) { iv = ivTopHabit2; tv = tvTopHabit2; }
        else             { iv = ivTopHabit3; tv = tvTopHabit3; }

        if (i >= sorted.size()) {
            iv.setVisibility(View.GONE);
            tv.setVisibility(View.GONE);
        } else {
            iv.setVisibility(View.VISIBLE);
            tv.setVisibility(View.VISIBLE);

            Habit h = sorted.get(i);
            iv.setImageResource(h.getIconResId());
            tv.setText("🔥 " + h.getStreakCount() + " days");
        }
    }

    /**
     * Load up to 3 friend keys from user.friends.
     * For each friend key => fetch friend user => show pic + username.
     * If user has no friends => "Add a friend!"
     */
    private void loadFriendsAndShowTop3(List<String> friendKeys) {
        if (friendKeys == null || friendKeys.isEmpty()) {
            // no friends
            tvNoFriendsMessage.setVisibility(View.VISIBLE);

            friendOne.setVisibility(View.GONE);
            friendOneName.setVisibility(View.GONE);

            friendTwo.setVisibility(View.GONE);
            friendTwoName.setVisibility(View.GONE);

            friendThree.setVisibility(View.GONE);
            friendThreeName.setVisibility(View.GONE);

            return;
        }
        // else show up to 3
        tvNoFriendsMessage.setVisibility(View.GONE);

        for (int i = 0; i < 3; i++) {
            if (i >= friendKeys.size()) {
                hideFriendSlot(i);
            } else {
                String friendKey = friendKeys.get(i);
                showFriendSlot(i, friendKey);
            }
        }
    }

    private void hideFriendSlot(int i) {
        if (i == 0) {
            friendOne.setVisibility(View.GONE);
            friendOneName.setVisibility(View.GONE);
        } else if (i == 1) {
            friendTwo.setVisibility(View.GONE);
            friendTwoName.setVisibility(View.GONE);
        } else {
            friendThree.setVisibility(View.GONE);
            friendThreeName.setVisibility(View.GONE);
        }
    }

    private void showFriendSlot(int i, String friendKey) {
        DatabaseReference friendRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(friendKey);

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friendUser = snapshot.getValue(User.class);
                if (friendUser == null) {
                    hideFriendSlot(i);
                    return;
                }

                CircleImageView friendImg;
                TextView friendNameTxt;
                if (i == 0) {
                    friendImg = friendOne;
                    friendNameTxt = friendOneName;
                } else if (i == 1) {
                    friendImg = friendTwo;
                    friendNameTxt = friendTwoName;
                } else {
                    friendImg = friendThree;
                    friendNameTxt = friendThreeName;
                }

                friendImg.setVisibility(View.VISIBLE);
                friendNameTxt.setVisibility(View.VISIBLE);

                String uname = !TextUtils.isEmpty(friendUser.username)
                        ? friendUser.username : "Friend";
                friendNameTxt.setText(uname);

                // Avatar
                int resourceId;
                if ("gamer".equals(friendUser.profilePicUrl)) {
                    resourceId = R.drawable.gamer;
                } else if ("man".equals(friendUser.profilePicUrl)) {
                    resourceId = R.drawable.man;
                } else if ("girl".equals(friendUser.profilePicUrl)) {
                    resourceId = R.drawable.girl;
                } else {
                    resourceId = R.drawable.unknown_profile;
                }
                friendImg.setImageResource(resourceId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideFriendSlot(i);
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
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingsActivity.class)));
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
                                                "Bio updated", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to update bio",
                                                Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // Let user toggle username
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
                        .inflate(R.layout.dialog_all_habits, null);

                RecyclerView rv = dialogView.findViewById(R.id.habit_list_view);
                rv.setLayoutManager(new LinearLayoutManager(getContext()));

                AllHabitsAdapter adapter = new AllHabitsAdapter(trackedHabits);
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

    private void showFriendsDialog() {

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User me = snapshot.getValue(User.class);
                if (me == null || me.friends == null || me.friends.isEmpty()) {
                    Toast.makeText(getContext(),
                            "You have no friends!", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> friendKeys = new ArrayList<>(me.friends);
                loadAllFriendUsers(friendKeys, new ArrayList<>());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAllFriendsWithAvatarsDialog(List<User> friendUsers) {
        if (getContext() == null) return;
        if (friendUsers.isEmpty()) {
            Toast.makeText(getContext(),
                    "No friends found", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_all_friends, null);

        RecyclerView rvFriends = dialogView.findViewById(R.id.rvAllFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        FriendsAdapter adapter = new FriendsAdapter(friendUsers);
        rvFriends.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("My Friends")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void loadAllFriendUsers(List<String> friendKeys, List<User> friendUsers) {
        if (friendKeys.isEmpty()) {
            showAllFriendsWithAvatarsDialog(friendUsers);
            return;
        }

        String firstKey = friendKeys.get(0);
        DatabaseReference friendRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(firstKey);

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friendUser = snapshot.getValue(User.class);
                if (friendUser != null) {
                    friendUsers.add(friendUser);
                }
                friendKeys.remove(0);
                loadAllFriendUsers(friendKeys, friendUsers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                friendKeys.remove(0);
                loadAllFriendUsers(friendKeys, friendUsers);
            }
        });
    }

    private void loadAllFriendNames(List<String> friendKeys, List<String> friendUsernames) {
        if (friendKeys.isEmpty()) {
            showAllFriendsDialog(friendUsernames);
            return;
        }

        String firstKey = friendKeys.get(0);
        DatabaseReference friendRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(firstKey);

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friendUser = snapshot.getValue(User.class);
                if (friendUser != null && !TextUtils.isEmpty(friendUser.username)) {
                    friendUsernames.add(friendUser.username);
                } else {
                    friendUsernames.add("Unknown friend");
                }
                friendKeys.remove(0);
                loadAllFriendNames(friendKeys, friendUsernames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                friendKeys.remove(0);
                loadAllFriendNames(friendKeys, friendUsernames);
            }
        });
    }

    private void showAllFriendsDialog(List<String> allFriendNames) {
        if (getContext() == null) return;
        if (allFriendNames.isEmpty()) {
            Toast.makeText(getContext(),
                    "No friends found", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = allFriendNames.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("My Friends")
                .setItems(items, (dialog, which) -> {
                    // If you want a click action
                })
                .setPositiveButton("Close", null)
                .show();
    }

    // For choosing avatar
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
            updateProfileAvatar("gamer", true);
            dialog.dismiss();
        });
        avatarMan.setOnClickListener(v -> {
            updateProfileAvatar("man", true);
            dialog.dismiss();
        });
        avatarGirl.setOnClickListener(v -> {
            updateProfileAvatar("girl", true);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateProfileAvatar(String avatarName, boolean showToast) {
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

        if (showToast) {
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
}
