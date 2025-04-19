package edu.northeastern.guildly;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.WeeklyChallengeManager;
import edu.northeastern.guildly.adapters.HabitAdapter;
import edu.northeastern.guildly.adapters.LeaderboardAdapter;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.LeaderboardItem;
import edu.northeastern.guildly.data.User;

/**
 * HomeFragment with enhanced weekly challenge functionality:
 * - Multiple completions per week (randomized target count)
 * - 24-hour cooldown between completions
 * - Weekly streak tracking
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView tvUserName, tvStreak, tvWeeklyChallenge, tvHabitsCount;
    private TextView tvChallengeStreak; // Added for weekly challenge streak display
    private ImageView weeklyChallengeIcon;
    private CheckBox weeklyChallengeCheckbox; // For clicking to complete weekly challenge
    private TextView weeklyChallengeLockedMsg; // For displaying locked message
    private RecyclerView habitRecyclerView, friendsLeaderboard;
    private Button btnAddHabit;

    private HabitAdapter habitAdapter;
    private final List<Habit> habitList = new ArrayList<>();

    private DatabaseReference userRef;       // /users/<myUserKey>
    private DatabaseReference userHabitsRef; // /users/<myUserKey>/habits
    private String myUserKey;
    private String myEmail;

    // WeeklyChallengeManager instance
    private WeeklyChallengeManager weeklyChallengeManager;

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

    public HomeFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        tvStreak = view.findViewById(R.id.textViewStreak);
        habitRecyclerView = view.findViewById(R.id.habit_list);
        btnAddHabit = view.findViewById(R.id.btn_add_habit);
        tvWeeklyChallenge = view.findViewById(R.id.weekly_challenge_text);
        weeklyChallengeIcon = view.findViewById(R.id.weekly_challenge_icon);
        tvChallengeStreak = view.findViewById(R.id.weekly_challenge_streak);
        weeklyChallengeCheckbox = view.findViewById(R.id.habit_item);
        weeklyChallengeLockedMsg = view.findViewById(R.id.lockMessage);
        friendsLeaderboard = view.findViewById(R.id.friendsleaderboard);
        tvHabitsCount = view.findViewById(R.id.habits_count);

        // Get current user info
        myEmail = MainActivity.currentUserEmail;
        if (!TextUtils.isEmpty(myEmail)) {
            myUserKey = myEmail.replace(".", ",");
            userRef = FirebaseDatabase.getInstance().getReference("users").child(myUserKey);
            userHabitsRef = userRef.child("habits");

            // Initialize weekly challenge manager with current user's email
            weeklyChallengeManager = new WeeklyChallengeManager(myEmail);

            // Check if current challenge is expired/needs updating
            weeklyChallengeManager.checkAndUpdateWeeklyChallenge(() -> {
                // Then load and display the weekly challenge
                loadWeeklyChallengeUI();
            });

            // Also load user info and leaderboard
            loadUserInfo();
            loadFriendsLeaderboard();
        } else {
            Toast.makeText(getContext(), "No user email found", Toast.LENGTH_SHORT).show();
        }

        // Set up main habit adapter in "HOME MODE" (isSelectionMode=false)
        habitAdapter = new HabitAdapter(habitList, userHabitsRef, false);
        habitRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        habitRecyclerView.setAdapter(habitAdapter);

        // Load habits both ways: single-value read plus real-time updates
        if (userHabitsRef != null) {
            loadHabitsFromFirebase();

            // Real-time updates via GuildlyDataManager
            GuildlyDataManager dataManager = GuildlyDataManager.getInstance();
            dataManager.init(myUserKey);

            dataManager.getHabitsLiveData().observe(getViewLifecycleOwner(), updatedHabits -> {
                habitList.clear();
                for (Habit h : updatedHabits) {
                    if (h.isTracked()) {
                        habitList.add(h);
                    }
                }
                habitAdapter.notifyDataSetChanged();
                updateStreakText();
                updateHabitsCountText();
            });
        }

        // Set up weekly challenge checkbox click listener
        weeklyChallengeCheckbox.setOnClickListener(v -> {
            // If the checkbox is checked, try to complete the weekly challenge
            if (weeklyChallengeCheckbox.isChecked()) {
                attemptWeeklyChallengeCompletion();
            }
        });

        // Add Habit button shows the predefined habits dialog
        btnAddHabit.setOnClickListener(v -> showPredefinedHabitsDialog());

        return view;
    }

    /**
     * Load and display the weekly challenge in the UI
     */
    private void loadWeeklyChallengeUI() {
        if (weeklyChallengeManager == null) return;

        // First, get the global challenge details (name, icon)
        weeklyChallengeManager.loadWeeklyChallenge(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String habitName = snapshot.child("habitName").getValue(String.class);
                Long iconResId = snapshot.child("iconResId").getValue(Long.class);

                if (habitName != null && iconResId != null) {
                    tvWeeklyChallenge.setText(habitName);
                    weeklyChallengeIcon.setImageResource(iconResId.intValue());

                    // Then get the user's progress on this challenge
                    loadWeeklyChallengeProgress();
                } else {
                    tvWeeklyChallenge.setText("No weekly challenge set");
                    weeklyChallengeCheckbox.setVisibility(View.GONE);
                    weeklyChallengeLockedMsg.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading weekly challenge", error.toException());
            }
        });
    }

    /**
     * Load the user's progress on the current weekly challenge
     */
    private void loadWeeklyChallengeProgress() {
        if (userRef == null) return;

        userRef.child("weeklyChallengeProgress").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get completion count and total required
                long completedCount = snapshot.child("completedCountThisWeek").getValue(Long.class) != null
                        ? snapshot.child("completedCountThisWeek").getValue(Long.class)
                        : 0;

                int requiredCompletions = 4; // Default value
                Long requiredLong = snapshot.child("requiredCompletions").getValue(Long.class);
                if (requiredLong != null) {
                    requiredCompletions = requiredLong.intValue();
                }

                // Get lockout state
                long nextAvailableTime = snapshot.child("nextAvailableTime").getValue(Long.class) != null
                        ? snapshot.child("nextAvailableTime").getValue(Long.class)
                        : 0;

                boolean fullyCompleted = snapshot.child("fullyCompleted").getValue(Boolean.class) != null
                        && snapshot.child("fullyCompleted").getValue(Boolean.class);

                // Get weekly streak count
                long streakCount = snapshot.child("streakCount").getValue(Long.class) != null
                        ? snapshot.child("streakCount").getValue(Long.class)
                        : 0;

                // Update the UI based on all this information
                updateWeeklyChallengeUI(completedCount, requiredCompletions, nextAvailableTime, fullyCompleted, streakCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading weekly challenge progress", error.toException());
            }
        });
    }

    /**
     * Update weekly challenge UI based on current progress
     */
    private void updateWeeklyChallengeUI(long completedCount, int requiredCompletions,
                                         long nextAvailableTime, boolean fullyCompleted, long streakCount) {

        // Update streak count display
        tvChallengeStreak.setText(String.format("Streak: %d", streakCount));

        long now = System.currentTimeMillis();

        // If fully completed for the week
        if (fullyCompleted) {
            weeklyChallengeCheckbox.setChecked(true);
            weeklyChallengeCheckbox.setEnabled(false);
            weeklyChallengeLockedMsg.setVisibility(View.VISIBLE);
            tvChallengeStreak.setVisibility(View.GONE);

            weeklyChallengeLockedMsg.setText("Come Back Next Week!");

            // Update progress display
//            tvChallengeStreak.setText(String.format("Come Back Next week!", streakCount));
            return;
        }

        // If on cooldown (next available time is in the future)
        if (now < nextAvailableTime) {
            weeklyChallengeCheckbox.setChecked(true);
            weeklyChallengeCheckbox.setEnabled(false);
            weeklyChallengeLockedMsg.setVisibility(View.VISIBLE);
            weeklyChallengeLockedMsg.setText("Come back in 24 hours");

            // Show progress and countdown
            tvChallengeStreak.setText(String.format("Weekly Progress: %d/%d",
                    streakCount, completedCount, requiredCompletions));
            return;
        }

        // If available to complete now
        weeklyChallengeCheckbox.setChecked(false);
        weeklyChallengeCheckbox.setEnabled(true);
        weeklyChallengeLockedMsg.setVisibility(View.GONE);

        // Show progress toward completion
        if (completedCount > 0) {
            tvChallengeStreak.setText(String.format("Weekly Progress: %d/%d",
                    streakCount, completedCount, requiredCompletions));
        } else {
            tvChallengeStreak.setText(String.format("Complete %d times this week",
                    streakCount, requiredCompletions));
        }
    }

    /**
     * Attempt to complete the weekly challenge
     */
    private void attemptWeeklyChallengeCompletion() {
        if (weeklyChallengeManager == null) return;

        weeklyChallengeManager.attemptWeeklyChallengeCompletion(message -> {
            // Show result message to the user
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

            // The UI will update automatically via the ValueEventListener
            // in loadWeeklyChallengeProgress()
        });
    }

    private void loadUserInfo() {
        if (userRef == null) return;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                // Update any user-specific UI here if needed
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadHabitsFromFirebase() {
        if (userHabitsRef == null) return;
        userHabitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                habitList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Habit h = ds.getValue(Habit.class);
                        if (h != null && h.isTracked()) {
                            habitList.add(h);
                        }
                    } catch (DatabaseException e) {
                        Log.d(TAG, "Skipping non-Habit entry: " + ds.getKey());
                    }
                }
                habitAdapter.notifyDataSetChanged();
                updateStreakText();
                updateHabitsCountText();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showPredefinedHabitsDialog() {
        List<Habit> cloneList = new ArrayList<>();
        for (Habit ph : predefinedHabits) {
            if (ph == null || ph.getHabitName() == null) {
                Log.e(TAG, "Found null habit or habit name in predefined habits");
                continue;
            }

            boolean alreadyTracked = false;
            Habit existingHabit = null;

            for (Habit current : habitList) {
                if (current == null || current.getHabitName() == null) {
                    continue;
                }

                if (current.getHabitName().equals(ph.getHabitName())) {
                    alreadyTracked = true;
                    existingHabit = current;
                    break;
                }
            }

            Habit newHabit = new Habit(ph.getHabitName(), ph.getIconResId());
            newHabit.setTracked(alreadyTracked);

            if (alreadyTracked && existingHabit != null) {
                newHabit.setStreakCount(existingHabit.getStreakCount());
                newHabit.setLastCompletedTime(existingHabit.getLastCompletedTime());
                newHabit.setCompletedToday(existingHabit.isCompletedToday());
                newHabit.setNextAvailableTime(existingHabit.getNextAvailableTime());
            } else {
                newHabit.setStreakCount(0);
                newHabit.setLastCompletedTime(0);
                newHabit.setCompletedToday(false);
                newHabit.setNextAvailableTime(0);
            }

            cloneList.add(newHabit);
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_predefined_habits, null);
        RecyclerView rv = dialogView.findViewById(R.id.predefinedHabitsRecycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        HabitAdapter tempAdapter = new HabitAdapter(cloneList, userHabitsRef, true);
        rv.setAdapter(tempAdapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Habits to Track")
                .setView(dialogView)
                .setPositiveButton("Done", (dialog, which) -> {
                    try {
                        for (Habit h : cloneList) {
                            if (h == null || h.getHabitName() == null) {
                                Log.e(TAG, "Skipping null habit or habit with null name");
                                continue;
                            }

                            String sanitizedName = h.getHabitName().replace(".", "_");
                            DatabaseReference habitRef = userHabitsRef.child(sanitizedName);

                            if (h.isTracked()) {
                                boolean existsInCurrent = false;
                                Habit existingHabit = null;

                                for (Habit current : habitList) {
                                    if (current == null || current.getHabitName() == null) {
                                        continue;
                                    }

                                    if (current.getHabitName().equals(h.getHabitName())) {
                                        existsInCurrent = true;
                                        existingHabit = current;
                                        break;
                                    }
                                }

                                if (existsInCurrent && existingHabit != null) {
                                    habitRef.child("tracked").setValue(true);
                                } else {
                                    habitRef.setValue(h);
                                }
                            } else {
                                habitRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            habitRef.child("tracked").setValue(false);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error checking habit: " + error.getMessage());
                                    }
                                });
                            }
                        }

                        loadHabitsFromFirebase();
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating habits: " + e.getMessage());
                        Toast.makeText(getContext(), "An error occurred while updating habits", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .create()
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void updateStreakText() {
        if (habitList.isEmpty()) {
            tvStreak.setText("Start a streak today!");
            return;
        }
        int bestStreak = 0;
        String bestHabitName = null;
        for (Habit h : habitList) {
            if (h.getStreakCount() > bestStreak) {
                bestStreak = h.getStreakCount();
                bestHabitName = h.getHabitName();
            }
        }
        if (bestStreak > 0 && bestHabitName != null) {
            tvStreak.setText(bestStreak + " day streak!");
        } else {
            tvStreak.setText("Start a streak today!");
        }
    }

    private void loadFriendsLeaderboard() {
        if (userRef == null) return;

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser == null || currentUser.friends == null || currentUser.friends.isEmpty()) return;

                List<String> friendKeys = currentUser.friends;
                List<LeaderboardItem> leaderboardItems = new ArrayList<>();
                final int totalFriends = friendKeys.size();
                final int[] loadedCount = {0};

                // Calculate current user's best streak
                int currentUserBestStreak = 0;
                DataSnapshot habitsSnapshot = snapshot.child("habits");
                for (DataSnapshot habitSnap : habitsSnapshot.getChildren()) {
                    Habit h = habitSnap.getValue(Habit.class);
                    if (h != null && h.getStreakCount() > currentUserBestStreak) {
                        currentUserBestStreak = h.getStreakCount();
                    }
                }

                // Add current user to leaderboard
                int profileImageRes = ProfileUtils.getProfileImageRes(currentUser.profilePicUrl);
                leaderboardItems.add(new LeaderboardItem(
                        currentUser.username != null ? currentUser.username : "Me",
                        currentUserBestStreak,
                        profileImageRes
                ));

                // Load each friend
                for (String friendKey : friendKeys) {
                    DatabaseReference friendRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(friendKey);

                    friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot ds) {
                            User friend = ds.getValue(User.class);
                            if (friend == null || friend.username == null) {
                                checkIfAllLoaded();
                                return;
                            }

                            int bestStreak = 0;
                            DataSnapshot friendHabitsSnapshot = ds.child("habits");
                            for (DataSnapshot habitSnap : friendHabitsSnapshot.getChildren()) {
                                Habit friendHabit = habitSnap.getValue(Habit.class);
                                if (friendHabit != null && friendHabit.getStreakCount() > bestStreak) {
                                    bestStreak = friendHabit.getStreakCount();
                                }
                            }

                            int profileImageRes = ProfileUtils.getProfileImageRes(friend.profilePicUrl);
                            leaderboardItems.add(new LeaderboardItem(friend.username, bestStreak, profileImageRes));
                            checkIfAllLoaded();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Leaderboard", "Failed to load friend: " + error.getMessage());
                            checkIfAllLoaded();
                        }

                        private void checkIfAllLoaded() {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalFriends) {
                                // Sort streaks descending
                                leaderboardItems.sort((a, b) ->
                                        Integer.compare(b.getStreakCount(), a.getStreakCount()));

                                // Optionally take top 3
                                List<LeaderboardItem> topItems = leaderboardItems.size() > 3
                                        ? leaderboardItems.subList(0, 3)
                                        : leaderboardItems;

                                // Update the Leaderboard UI
                                LeaderboardAdapter adapter = new LeaderboardAdapter(topItems);
                                friendsLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
                                friendsLeaderboard.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Leaderboard", "Failed to load current user: " + error.getMessage());
            }
        });
    }

    private void updateHabitsCountText() {
        int incompleteCount = 0;
        for (Habit h : habitList) {
            if (!h.isCompletedToday()) {
                incompleteCount++;
            }
        }

        if (incompleteCount == 0) {
            tvHabitsCount.setText("🎉 All habits done! Come back in 24 hours!");
        } else {
            tvHabitsCount.setText(incompleteCount + " habit" + (incompleteCount == 1 ? "" : "s")
                    + " left today to complete. Come back in 24 hours!");
        }
    }
}