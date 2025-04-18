package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import edu.northeastern.guildly.adapters.FriendsDialogAdapter;
import edu.northeastern.guildly.adapters.HabitAdapter;
import edu.northeastern.guildly.data.Chats;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.data.User;

/**
 * ProfileFragment that shows:
 *  - username, about me
 *  - top 3 habits by streak
 *  - up to 3 friends (profile pic + username)
 *  - "View More" for the full friend list
 *  - "View More" for the full (predefined) habit list
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

    // ---------------------------------------------------------------
    // ADDED: replicate the “HomeFragment” approach
    // ---------------------------------------------------------------
    private final List<Habit> predefinedHabits = Arrays.asList(
            new Habit("Drink 64oz of water", R.drawable.ic_water),
            new Habit("Workout for 30 mins", R.drawable.ic_workout),
            new Habit("Do homework", R.drawable.ic_homework),
            new Habit("Read a book", R.drawable.ic_book),
            new Habit("Meditate for 10 minutes", R.drawable.ic_meditation),
            new Habit("Save money today", R.drawable.ic_savemoney),
            new Habit("Eat vegetables", R.drawable.ic_vegetable),
            new Habit("No phone after 10PM", R.drawable.ic_phonebanned)
    );

    private final List<Habit> habitList = new ArrayList<>(); // The user’s current habits
    private DatabaseReference userHabitsRef;                 // i.e., /users/<myUserKey>/habits

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
        userHabitsRef = userRef.child("habits");

        // Initialize
        initViews(view);
        // Single-value approach to load user data
        loadUserDataFromFirebase();
        // Then set click listeners
        setupClickListeners();

        // OPTIONAL: If you also want real-time updates with GuildlyDataManager:
        if (!"NO_USER".equals(myUserKey)) {
            GuildlyDataManager manager = GuildlyDataManager.getInstance();
            manager.init(myUserKey);

            // Observe habits => auto update the top 3
            manager.getHabitsLiveData().observe(getViewLifecycleOwner(), updatedHabits -> {
                if (updatedHabits == null) return;

                List<Habit> tracked = new ArrayList<>();
                for (Habit h : updatedHabits) {
                    if (h != null && h.isTracked()) {
                        tracked.add(h);
                    }
                }
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
                    Collections.sort(tracked, (o1, o2) -> o2.getStreakCount() - o1.getStreakCount());

                    Habit top = tracked.get(0);
                    if (top.getStreakCount() > 0) {
                        streakDescription.setText("Longest streak: " +
                                top.getStreakCount() + " days of " +
                                top.getHabitName() + "!");
                    } else {
                        streakDescription.setText("No streak yet!");
                    }

                    setTopHabitSlot(0, tracked);
                    setTopHabitSlot(1, tracked);
                    setTopHabitSlot(2, tracked);
                }
            });
        }

        // Also load the user’s current habitList once
        loadCurrentHabits();
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
     * 1) Single-value approach: load user info, top 3 habits & top 3 friends
     */
    private void loadUserDataFromFirebase() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) return;

                // Username
                profileUsername.setText(TextUtils.isEmpty(user.username)
                        ? "UnnamedUser" : user.username);

                // About me
                if (!TextUtils.isEmpty(user.aboutMe)) {
                    profileAboutMe.setText(user.aboutMe);
                } else {
                    profileAboutMe.setText("Add a bio...");
                }

                // Avatar
                if (!TextUtils.isEmpty(user.profilePicUrl)) {
                    updateProfileAvatar(user.profilePicUrl, false);
                }

                // Now load top 3 habits
                loadTrackedHabitsAndSort();
                // Now load top 3 friends
                loadFriendsAndShowTop3(user.friends);
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
     * 2) Single-value read to fill top-3 habits
     */
    private void loadTrackedHabitsAndSort() {
        userHabitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                    ivTopHabit1.setVisibility(View.GONE);
                    ivTopHabit2.setVisibility(View.GONE);
                    ivTopHabit3.setVisibility(View.GONE);
                    tvTopHabit1.setVisibility(View.GONE);
                    tvTopHabit2.setVisibility(View.GONE);
                    tvTopHabit3.setVisibility(View.GONE);
                    streakDescription.setText("No streak yet!");
                    return;
                }

                tvNoHabitsMessage.setVisibility(View.GONE);
                Collections.sort(tracked, (o1, o2) -> o2.getStreakCount() - o1.getStreakCount());

                Habit top = tracked.get(0);
                if (top.getStreakCount() > 0) {
                    streakDescription.setText("Longest streak: " +
                            top.getStreakCount() + " days of " +
                            top.getHabitName() + "!");
                } else {
                    streakDescription.setText("No streak yet!");
                }

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
        if (i == 0) {
            iv = ivTopHabit1;
            tv = tvTopHabit1;
        } else if (i == 1) {
            iv = ivTopHabit2;
            tv = tvTopHabit2;
        } else {
            iv = ivTopHabit3;
            tv = tvTopHabit3;
        }

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
     * 3) Single-value read to load top-3 friends. (Unchanged from your code.)
     */
    private void loadFriendsAndShowTop3(List<String> friendKeys) {
        if (friendKeys == null || friendKeys.isEmpty()) {
            tvNoFriendsMessage.setVisibility(View.VISIBLE);
            friendOne.setVisibility(View.GONE);
            friendOneName.setVisibility(View.GONE);
            friendTwo.setVisibility(View.GONE);
            friendTwoName.setVisibility(View.GONE);
            friendThree.setVisibility(View.GONE);
            friendThreeName.setVisibility(View.GONE);
            return;
        }

        tvNoFriendsMessage.setVisibility(View.GONE);

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Create a map of friendKey -> lastMessageTimestamp
                Map<String, Long> friendLastMessageMap = new HashMap<>();

                // For each chat
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    Chats chatObj = chatSnap.getValue(Chats.class);
                    if (chatObj == null || chatObj.participants == null) continue;

                    // If the current user is a participant
                    if (chatObj.participants.contains(myUserKey)) {
                        // Find which participant is the friend
                        String friendKey = null;
                        for (String participant : chatObj.participants) {
                            if (!participant.equals(myUserKey) && friendKeys.contains(participant)) {
                                friendKey = participant;
                                break;
                            }
                        }

                        // If found a friend and there are messages
                        if (friendKey != null && chatObj.messages != null && !chatObj.messages.isEmpty()) {
                            // Find the most recent message timestamp
                            long maxTime = -1;
                            for (Map.Entry<String, Message> entry : chatObj.messages.entrySet()) {
                                Message msg = entry.getValue();
                                if (msg.timestamp > maxTime) {
                                    maxTime = msg.timestamp;
                                }
                            }

                            // Store the max timestamp for this friend
                            if (maxTime > -1) {
                                if (friendLastMessageMap.containsKey(friendKey)) {
                                    if (maxTime > friendLastMessageMap.get(friendKey)) {
                                        friendLastMessageMap.put(friendKey, maxTime);
                                    }
                                } else {
                                    friendLastMessageMap.put(friendKey, maxTime);
                                }
                            }
                        }
                    }
                }

                // Sort by timestamp (most recent first)
                List<Map.Entry<String, Long>> sortedFriends = new ArrayList<>(friendLastMessageMap.entrySet());
                Collections.sort(sortedFriends, (e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

                // Get ordered friend keys
                List<String> orderedFriends = new ArrayList<>();
                for (Map.Entry<String, Long> entry : sortedFriends) {
                    orderedFriends.add(entry.getKey());
                }

                // Add any friends who don't have messages yet
                for (String friendKey : friendKeys) {
                    if (!orderedFriends.contains(friendKey)) {
                        orderedFriends.add(friendKey);
                    }
                }

                // Display up to 3 friends
                for (int i = 0; i < 3 && i < orderedFriends.size(); i++) {
                    showFriendSlot(i, orderedFriends.get(i));
                }

                // Hide any unused slots
                for (int i = orderedFriends.size(); i < 3; i++) {
                    hideFriendSlot(i);
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProfileFragment", "Error loading chat history", error.toException());

                // Fallback: just show friends without sorting by recency
                for (int i = 0; i < 3 && i < friendKeys.size(); i++) {
                    showFriendSlot(i, friendKeys.get(i));
                }

                // Hide any unused slots
                for (int i = friendKeys.size(); i < 3; i++) {
                    hideFriendSlot(i);
                }
            }
        });
    }
    // ---------------------------------------------------------
    //  ADDED: load user’s current habits into habitList once
    // ---------------------------------------------------------
    private void loadCurrentHabits() {
        userHabitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                habitList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Habit h = ds.getValue(Habit.class);
                    if (h != null) {
                        habitList.add(h);
                    }
                }
                }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProfileFragment", "Error loading user’s habitList: " + error.getMessage());
            }
        });
    }

    // ---------------------------------------------------------
    //  REPLACE showHabitsDialog() with the “predefined” approach
    // ---------------------------------------------------------
    private void showHabitsDialog() {
        // Step 1) Build a cloneList from predefinedHabits
        List<Habit> cloneList = new ArrayList<>();
        for (Habit ph : predefinedHabits) {
            if (ph == null || ph.getHabitName() == null) {
                Log.e("ProfileFragment", "Skipping null habit or habit name in predefinedHabits");
                continue;
            }

            boolean alreadyTracked = false;
            Habit existingHabit = null;

            // check user’s current habitList
            for (Habit current : habitList) {
                if (current != null && current.getHabitName() != null &&
                        current.getHabitName().equals(ph.getHabitName())) {
                    alreadyTracked = true;
                    existingHabit = current;
                    break;
                }
            }

            // create a new Habit with the same name/icon
            Habit newHabit = new Habit(ph.getHabitName(), ph.getIconResId());
            newHabit.setTracked(alreadyTracked);

            // if user already tracked it, copy streak data
            if (alreadyTracked && existingHabit != null) {
                newHabit.setStreakCount(existingHabit.getStreakCount());
                newHabit.setLastCompletedTime(existingHabit.getLastCompletedTime());
                newHabit.setCompletedToday(existingHabit.isCompletedToday());
                newHabit.setNextAvailableTime(existingHabit.getNextAvailableTime());
            } else {
                // brand new
                newHabit.setStreakCount(0);
                newHabit.setLastCompletedTime(0);
                newHabit.setCompletedToday(false);
                newHabit.setNextAvailableTime(0);
            }

            cloneList.add(newHabit);
        }

        // Step 2) Inflate dialog_predefined_habits
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_predefined_habits, null);
        RecyclerView rv = dialogView.findViewById(R.id.predefinedHabitsRecycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Step 3) Use HabitAdapter in selection mode
        HabitAdapter tempAdapter = new HabitAdapter(cloneList, userHabitsRef, true);
        rv.setAdapter(tempAdapter);

        // Step 4) Show AlertDialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Habits to Track")
                .setView(dialogView)
                .setPositiveButton("Done", (dialog, which) -> {
                    try {
                        // Step 5) For each habit in cloneList => update DB
                        for (Habit h : cloneList) {
                            if (h == null || h.getHabitName() == null) {
                                Log.e("ProfileFragment", "Skipping null habit or habit with null name");
                                continue;
                            }

                            String sanitizedName = h.getHabitName().replace(".", "_");
                            DatabaseReference habitRef = userHabitsRef.child(sanitizedName);

                            if (h.isTracked()) {
                                boolean existsInCurrent = false;
                                Habit existingHabit = null;

                                for (Habit current : habitList) {
                                    if (current != null && current.getHabitName() != null &&
                                            current.getHabitName().equals(h.getHabitName())) {
                                        existsInCurrent = true;
                                        existingHabit = current;
                                        break;
                                    }
                                }

                                if (existsInCurrent && existingHabit != null) {
                                    // just set tracked=true
                                    habitRef.child("tracked").setValue(true);
                                } else {
                                    // brand new => write entire Habit
                                    habitRef.setValue(h);
                                }
                            } else {
                                // user unticked => set tracked=false if it exists
                                habitRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            habitRef.child("tracked").setValue(false);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("ProfileFragment", "Error updating habit: " + error.getMessage());
                                    }
                                });
                            }
                        }

                        // Refresh local habitList
                        loadCurrentHabits();

                    } catch (Exception e) {
                        Log.e("ProfileFragment", "Error in finalizing habit selection: " + e.getMessage());
                        Toast.makeText(getContext(),
                                "An error occurred while updating habits",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void confirmDeleteFriend(String friendKey) {
        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Friend")
                .setMessage("Are you sure you want to remove this friend?")
                .setPositiveButton("Yes", (dialogInterface, i) -> deleteFriend(friendKey))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteFriend(String friendKey) {
        userRef.child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                List<String> friendsList = new ArrayList<>();
                for (DataSnapshot friendSnap : snapshot.getChildren()) {
                    String existingFriendKey = friendSnap.getValue(String.class);
                    if (existingFriendKey != null && !existingFriendKey.equals(friendKey)) {
                        friendsList.add(existingFriendKey);
                    }
                }

                // Update only the friends list
                userRef.child("friends").setValue(friendsList)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(getContext(),
                                        "Friend removed", Toast.LENGTH_SHORT).show());

                // 2) Remove myUserKey from the friend's friend list
                DatabaseReference friendRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(friendKey);

                friendRef.child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot friendSnap) {
                        if (!friendSnap.exists()) return;

                        List<String> theirFriendsList = new ArrayList<>();
                        for (DataSnapshot theirFriendSnap : friendSnap.getChildren()) {
                            String theirExistingFriendKey = theirFriendSnap.getValue(String.class);
                            if (theirExistingFriendKey != null && !theirExistingFriendKey.equals(myUserKey)) {
                                theirFriendsList.add(theirExistingFriendKey);
                            }
                        }

                        // Update only their friends list
                        friendRef.child("friends").setValue(theirFriendsList)
                                .addOnSuccessListener(aVoid -> {
                                    // Reload profile data to refresh the UI
                                    loadUserDataFromFirebase();
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void findOrCreateChatThenOpen(String friendKey) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        // Do a single read of all chats to find one that has these two participants
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingChatId = null;

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    // read the chat object
                    Chats chatObj = chatSnap.getValue(Chats.class);
                    if (chatObj == null || chatObj.participants == null) continue;

                    if (chatObj.participants.size() == 2
                            && chatObj.participants.contains(myUserKey)
                            && chatObj.participants.contains(friendKey)) {
                        existingChatId = chatObj.chatId;
                        break;
                    }
                }

                if (existingChatId != null) {
                    // Use existing chat detail
                    openChatDetail(existingChatId, friendKey);
                } else {
                    // Create new chat
                    String newChatId = chatsRef.push().getKey();
                    if (newChatId == null) {
                        Toast.makeText(getContext(),
                                "Error creating chat", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Chats newChat = new Chats();
                    newChat.chatId = newChatId;
                    List<String> parts = new ArrayList<>();
                    parts.add(myUserKey);
                    parts.add(friendKey);
                    newChat.participants = parts;

                    chatsRef.child(newChatId).setValue(newChat)
                            .addOnSuccessListener(aVoid -> {
                                openChatDetail(newChatId, friendKey);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(),
                                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error finding chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatDetail(String chatId, String friendKey) {
        // Optionally fetch the friend's username for the title
        DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference("users")
                .child(friendKey)
                .child("username");

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String friendUsername = snapshot.getValue(String.class);
                if (friendUsername == null) friendUsername = "Friend";

                // Cast to AppCompatActivity before calling
                if (getActivity() instanceof AppCompatActivity) {
                    ChatDetailActivity.openChatDetail(
                            (AppCompatActivity) getActivity(),
                            chatId,
                            friendUsername
                    );
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void setupClickListeners() {
        // “View More” for HABITS => calls your new showHabitsDialog()
        habitsViewMore.setOnClickListener(v -> showHabitsDialog());


        friendsViewMore.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AllFriendsActionsActivity.class);
            startActivity(intent);
        });

        profileEditButton.setOnClickListener(v -> toggleUsernameEditing());
        profileImage.setOnClickListener(v -> showSelectAvatarDialog());

        View aboutMeCard = getView().findViewById(R.id.about_me_card);
        aboutMeCard.setOnClickListener(v -> showEditAboutMeDialog());
        aboutMeEditButton.setOnClickListener(v -> showEditAboutMeDialog());
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingsActivity.class)));
    }

    private void showFriendsWithActionsDialog() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User me = snapshot.getValue(User.class);
                if (me == null || me.friends == null || me.friends.isEmpty()) {
                    Toast.makeText(getContext(),
                            "You have no friends!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create dialog view
                View dialogView = LayoutInflater.from(getContext())
                        .inflate(R.layout.dialog_all_friends_actions, null);

                RecyclerView rvFriends = dialogView.findViewById(R.id.recyclerViewAllFriends);
                rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

                // Set max height (optional)
                int maxHeightInDp = 400;
                float density = getResources().getDisplayMetrics().density;
                int maxHeightInPx = (int) (maxHeightInDp * density);
                rvFriends.getLayoutParams().height = maxHeightInPx;


                FriendsDialogAdapter adapter = new FriendsDialogAdapter(
                        me.friends,
                        new FriendsDialogAdapter.OnFriendActionListener() {
                            @Override
                            public void onProfileClick(String friendKey) {
                                if (getActivity() instanceof AppCompatActivity) {
                                    FriendProfileActivity.openProfile(
                                            (AppCompatActivity) getActivity(), friendKey);
                                }
                            }

                            @Override
                            public void onMessageClick(String friendKey) {
                                findOrCreateChatThenOpen(friendKey);
                            }

                            @Override
                            public void onDeleteClick(String friendKey) {
                                confirmDeleteFriend(friendKey);
                            }
                        }
                );

                rvFriends.setAdapter(adapter);

                // Show the dialog
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("My Friends")
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .create();

                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading friends", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(getContext(),
                                        "Username updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(),
                                        "Failed to update username", Toast.LENGTH_SHORT).show());
            }
            profileUsername.setEnabled(false);
            profileEditButton.setImageResource(R.drawable.ic_edit);
        }
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
                                    "Failed to update avatar", Toast.LENGTH_SHORT).show());
        }
    }
}
