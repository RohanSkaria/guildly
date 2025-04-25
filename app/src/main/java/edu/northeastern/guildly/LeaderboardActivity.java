package edu.northeastern.guildly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.northeastern.guildly.adapters.LeaderboardAdapter;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.LeaderboardItem;
import edu.northeastern.guildly.data.User;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView leaderboardRecycler;
    private LeaderboardAdapter leaderboardAdapter;

    private DatabaseReference usersRef;
    private String myEmail;
    private String myUserKey;

    // We'll hold the final list (top 10)
    private List<LeaderboardItem> leaderboardItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Instead of a typical "activity_leaderboard" layout,
        // we will use your existing "leaderboard_container" layout.
        setContentView(R.layout.leaderboard_layout);

        myEmail = MainActivity.currentUserEmail;
        if (myEmail != null) {
            myUserKey = myEmail.replace(".", ",");
        } else {
            Toast.makeText(this, "No user email found; please log in first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize UI elements
        leaderboardRecycler = findViewById(R.id.leaderboard_recycler_view);

        leaderboardItems = new ArrayList<>();
        leaderboardAdapter = new LeaderboardAdapter(leaderboardItems);
        leaderboardRecycler.setAdapter(leaderboardAdapter);
        leaderboardRecycler.setLayoutManager(new LinearLayoutManager(this));

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Build the leaderboard
        fetchFriendsAndBuildLeaderboard();
    }

    private void fetchFriendsAndBuildLeaderboard() {
        // Grab "friends" list from your current user
        DatabaseReference myRef = usersRef.child(myUserKey);
        myRef.child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> friendKeys = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String friendKey = ds.getValue(String.class);
                        if (friendKey != null) {
                            friendKeys.add(friendKey);
                        }
                    }
                }
                // Now we have all friend keys; let's retrieve each friend's top streak
                // Also add the current user so you see your own ranking
                friendKeys.add(myUserKey);
                loadFriendsTopStreaks(friendKeys);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LeaderboardActivity.this, "Error loading friends.", Toast.LENGTH_SHORT).show();
                Log.e("LeaderboardActivity", "fetchFriendsAndBuildLeaderboard canceled", error.toException());
            }
        });
    }

    private void loadFriendsTopStreaks(List<String> friendKeys) {
        final int totalCount = friendKeys.size();
        final int[] processedCount = {0};

        for (String friendKey : friendKeys) {
            usersRef.child(friendKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // find the friend’s top streak
                        int bestStreak = 0;
                        DataSnapshot habitsSnap = snapshot.child("habits");
                        if (habitsSnap.exists()) {
                            for (DataSnapshot habitDS : habitsSnap.getChildren()) {
                                Habit habit = habitDS.getValue(Habit.class);
                                if (habit != null && habit.isTracked() && habit.getStreakCount() > bestStreak) {
                                    bestStreak = habit.getStreakCount();
                                }
                            }
                        }

                        String usernameToShow = (user.username != null) ? user.username : friendKey;
                        int profileImageRes = ProfileUtils.getProfileImageRes(user.profilePicUrl);

                        leaderboardItems.add(new LeaderboardItem(usernameToShow, bestStreak, profileImageRes));
                    }

                    processedCount[0]++;
                    if (processedCount[0] == totalCount) {
                        finishAndDisplay();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    processedCount[0]++;
                    if (processedCount[0] == totalCount) {
                        finishAndDisplay();
                    }
                }
            });
        }
    }

    private void finishAndDisplay() {
        // Sort by streak descending
        Collections.sort(leaderboardItems, (a, b) -> Integer.compare(b.getStreakCount(), a.getStreakCount()));

        // Limit to top 10
        if (leaderboardItems.size() > 10) {
            leaderboardItems = leaderboardItems.subList(0, 10);
        }

        // Update adapter
        leaderboardAdapter.setLeaderboardItems(leaderboardItems);
        leaderboardAdapter.notifyDataSetChanged();
    }
}

