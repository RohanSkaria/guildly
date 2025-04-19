package edu.northeastern.guildly;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
 * Your HomeFragment now updates in real time via GuildlyDataManager,
 * while keeping all existing code intact, and using the new WeeklyChallengeManager(userEmail).
 */
public class HomeFragment extends Fragment {

    private TextView tvUserName, tvStreak, tvWeeklyChallenge, tvHabitsCount;
    private ImageView weeeklyChallengeIcon;
    private RecyclerView habitRecyclerView, friendsLeaderboard;
    private Button btnAddHabit;

    private HabitAdapter habitAdapter;
    private final List<Habit> habitList = new ArrayList<>();

    private DatabaseReference userRef;       // /users/<myUserKey>
    private DatabaseReference userHabitsRef; // /users/<myUserKey>/habits
    private String myUserKey;

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

    // (Optional leftover) This list is no longer used for the actual weekly logic,
    // since the new WeeklyChallengeManager does the picking.
    private final List<Habit> weeklyChallengeOptions = Arrays.asList(
            new Habit("Take a walk outside", R.drawable.ic_walk_icon),
            new Habit("Drink tea instead of coffee", R.drawable.ic_tea),
            new Habit("Compliment someone", R.drawable.ic_compliment),
            new Habit("Journal for 5 minutes", R.drawable.ic_journal),
            new Habit("No social media today", R.drawable.ic_nosocial),
            new Habit("Stretch for 10 minutes", R.drawable.ic_stretch),
            new Habit("Sleep 8+ hours", R.drawable.ic_sleep)
    );

    // New WeeklyChallengeManager that requires userEmail in constructor
    private WeeklyChallengeManager weeklyChallengeManager;

    public HomeFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Find views
        // tvUserName = view.findViewById(R.id.user_name);  // uncomment if you use it
        tvStreak = view.findViewById(R.id.textViewStreak);
        habitRecyclerView = view.findViewById(R.id.habit_list);
        btnAddHabit = view.findViewById(R.id.btn_add_habit);
        tvWeeklyChallenge = view.findViewById(R.id.weekly_challenge_text);
        weeeklyChallengeIcon = view.findViewById(R.id.weekly_challenge_icon);
        friendsLeaderboard = view.findViewById(R.id.friendsleaderboard);
        tvHabitsCount = view.findViewById(R.id.habits_count);

        // Get current user email from MainActivity
        String myEmail = MainActivity.currentUserEmail;
        if (!TextUtils.isEmpty(myEmail)) {
            // Convert email to a safe Firebase key
            myUserKey = myEmail.replace(".", ",");
            userRef = FirebaseDatabase.getInstance().getReference("users").child(myUserKey);
            userHabitsRef = userRef.child("habits");

            // 1) Create the new manager with userEmail
            weeklyChallengeManager = new WeeklyChallengeManager(myEmail);

            // 2) Check/update the global challenge if needed
            weeklyChallengeManager.checkAndUpdateWeeklyChallenge(() -> {
                // 3) Load and display the final challenge in the UI
                weeklyChallengeManager.loadWeeklyChallenge(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String habitName = snapshot.child("habitName").getValue(String.class);
                        Long iconResId = snapshot.child("iconResId").getValue(Long.class);
                        if (habitName != null && iconResId != null) {
                            tvWeeklyChallenge.setText(habitName);
                            weeeklyChallengeIcon.setImageResource(iconResId.intValue());
                        } else {
                            tvWeeklyChallenge.setText("No weekly challenge set");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle errors if needed
                    }
                });
            });

            // Also load user info and leaderboard
            loadUserInfo();
            loadFriendsLeaderboard();

        } else {
            // If no valid email
            // if (tvUserName != null) tvUserName.setText("Welcome, Guest!");
            Toast.makeText(getContext(), "No user email found; can't load challenge or habits.", Toast.LENGTH_SHORT).show();
        }

        // Initialize your main HabitAdapter in "HOME MODE" => isSelectionMode=false
        habitAdapter = new HabitAdapter(habitList, userHabitsRef, false);
        habitRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        habitRecyclerView.setAdapter(habitAdapter);

        // Attach single-value read for habits
        if (userHabitsRef != null) {
            loadHabitsFromFirebase();

            // Also attach GuildlyDataManager for real-time updates
            GuildlyDataManager dataManager = GuildlyDataManager.getInstance();
            dataManager.init(myUserKey);

            dataManager.getHabitsLiveData().observe(getViewLifecycleOwner(), new Observer<List<Habit>>() {
                @Override
                public void onChanged(List<Habit> updatedHabits) {
                    habitList.clear();
                    for (Habit h : updatedHabits) {
                        if (h.isTracked()) {
                            habitList.add(h);
                        }
                    }
                    habitAdapter.notifyDataSetChanged();
                    updateStreakText();
                    updateHabitsCountText();
                }
            });
        }

        // "Add Habit" button -> show the predefined habits popup
        btnAddHabit.setOnClickListener(v -> showPredefinedHabitsDialog());

        return view;
    }

    private void loadUserInfo() {
        if (userRef == null) return;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    // if (tvUserName != null && user.username != null) {
                    //     tvUserName.setText("Welcome, " + user.username + "!");
                    // }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Fetch the user's habits once, ignoring real-time changes (we do that separately).
     */
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
                        Log.d("HomeFragment", "Skipping non-Habit entry: " + ds.getKey());
                    }
                }
                habitAdapter.notifyDataSetChanged();
                updateStreakText();
                updateHabitsCountText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Popup to pick from a list of predefined habits and track/untrack them.
     */
    private void showPredefinedHabitsDialog() {
        List<Habit> cloneList = new ArrayList<>();
        for (Habit ph : predefinedHabits) {
            if (ph == null || ph.getHabitName() == null) continue;

            boolean alreadyTracked = false;
            Habit existingHabit = null;

            for (Habit current : habitList) {
                if (current != null && current.getHabitName() != null &&
                        current.getHabitName().equals(ph.getHabitName())) {
                    alreadyTracked = true;
                    existingHabit = current;
                    break;
                }
            }

            // Create a new Habit object for the dialog
            Habit newHabit = new Habit(ph.getHabitName(), ph.getIconResId());
            newHabit.setTracked(alreadyTracked);

            if (alreadyTracked && existingHabit != null) {
                // Copy streak data, etc.
                newHabit.setStreakCount(existingHabit.getStreakCount());
                newHabit.setLastCompletedTime(existingHabit.getLastCompletedTime());
                newHabit.setCompletedToday(existingHabit.isCompletedToday());
                newHabit.setNextAvailableTime(existingHabit.getNextAvailableTime());
            } else {
                // Default
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

        // "Selection mode" = true
        HabitAdapter tempAdapter = new HabitAdapter(cloneList, userHabitsRef, true);
        rv.setAdapter(tempAdapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Habits to Track")
                .setView(dialogView)
                .setPositiveButton("Done", (dialog, which) -> {
                    try {
                        for (Habit h : cloneList) {
                            if (h == null || h.getHabitName() == null) continue;
                            String sanitizedName = h.getHabitName().replace(".", "_");
                            DatabaseReference habitRef = userHabitsRef.child(sanitizedName);

                            if (h.isTracked()) {
                                // If newly tracked (or already existed)
                                boolean existsInCurrent = false;
                                for (Habit current : habitList) {
                                    if (current != null && current.getHabitName() != null &&
                                            current.getHabitName().equals(h.getHabitName())) {
                                        existsInCurrent = true;
                                        break;
                                    }
                                }

                                if (existsInCurrent) {
                                    habitRef.child("tracked").setValue(true);
                                } else {
                                    habitRef.setValue(h);
                                }
                            } else {
                                // If untracked
                                habitRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            habitRef.child("tracked").setValue(false);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("HomeFragment", "Error checking habit: " + error.getMessage());
                                    }
                                });
                            }
                        }
                        loadHabitsFromFirebase();
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error updating habits: " + e.getMessage());
                        Toast.makeText(getContext(), "An error occurred while updating habits",
                                Toast.LENGTH_SHORT).show();
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
            if (bestStreak == 1) {
                tvStreak.setText(bestStreak + " day streak!");
            }
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
                int currentUserProfileImageRes = R.drawable.gamer;
                leaderboardItems.add(new LeaderboardItem(
                        currentUser.username != null ? currentUser.username : "Me",
                        currentUserBestStreak,
                        currentUserProfileImageRes
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

                            int profileImageRes = R.drawable.gamer; // or any default icon
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

    /**
     * Update the "X habits left" text at the bottom.
     */
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
