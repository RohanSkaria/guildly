package edu.northeastern.guildly;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.User;
import edu.northeastern.guildly.R;

public class FriendProfileActivity extends AppCompatActivity {

    private static final String EXTRA_FRIEND_KEY = "FRIEND_KEY";

    private CircleImageView friendProfileImage;
    private TextView textFriendUsername, textFriendAboutMe;
    private TextView friendStreakDescription;
    private TextView tvNoHabitsMessage;

    private ImageView ivFriendTopHabit1, ivFriendTopHabit2, ivFriendTopHabit3;
    private TextView tvFriendTopHabit1, tvFriendTopHabit2, tvFriendTopHabit3;

    private String friendKey;
    private DatabaseReference friendRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // enable back arrow
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Friend Profile");
        }

        // Initialize basic profile views
        friendProfileImage = findViewById(R.id.friendProfileImage);
        textFriendUsername = findViewById(R.id.textFriendUsername);
        textFriendAboutMe = findViewById(R.id.textFriendAboutMe);
        friendStreakDescription = findViewById(R.id.friendStreakDescription);
        tvNoHabitsMessage = findViewById(R.id.tvNoHabitsMessage);

        // Initialize habit views
        ivFriendTopHabit1 = findViewById(R.id.ivFriendTopHabit1);
        ivFriendTopHabit2 = findViewById(R.id.ivFriendTopHabit2);
        ivFriendTopHabit3 = findViewById(R.id.ivFriendTopHabit3);
        tvFriendTopHabit1 = findViewById(R.id.tvFriendTopHabit1);
        tvFriendTopHabit2 = findViewById(R.id.tvFriendTopHabit2);
        tvFriendTopHabit3 = findViewById(R.id.tvFriendTopHabit3);

        friendKey = getIntent().getStringExtra(EXTRA_FRIEND_KEY);
        if (TextUtils.isEmpty(friendKey)) {
            Toast.makeText(this, "No friend key provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        friendRef = FirebaseDatabase.getInstance().getReference("users").child(friendKey);

        loadFriendProfile();
        loadFriendHabits();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // handle back arrow
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFriendProfile() {
        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friendUser = snapshot.getValue(User.class);
                if (friendUser == null) {
                    Toast.makeText(FriendProfileActivity.this,
                            "Friend not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (!TextUtils.isEmpty(friendUser.username)) {
                    textFriendUsername.setText(friendUser.username);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(friendUser.username);
                    }
                }

                if (!TextUtils.isEmpty(friendUser.aboutMe)) {
                    textFriendAboutMe.setText(friendUser.aboutMe);
                }

                // set avatar
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
                friendProfileImage.setImageResource(resourceId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FriendProfileActivity.this,
                        "Error loading friend data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFriendHabits() {
        friendRef.child("habits").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Habit> trackedHabits = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Habit habit = ds.getValue(Habit.class);
                        if (habit != null && habit.isTracked()) {
                            trackedHabits.add(habit);
                        }
                    } catch (Exception e) {
                        // Skip entries that can't be converted to Habit objects
                    }
                }

                // Update UI based on habits
                if (trackedHabits.isEmpty()) {
                    // Show "no habits" message
                    tvNoHabitsMessage.setVisibility(View.VISIBLE);
                    ivFriendTopHabit1.setVisibility(View.GONE);
                    ivFriendTopHabit2.setVisibility(View.GONE);
                    ivFriendTopHabit3.setVisibility(View.GONE);
                    tvFriendTopHabit1.setVisibility(View.GONE);
                    tvFriendTopHabit2.setVisibility(View.GONE);
                    tvFriendTopHabit3.setVisibility(View.GONE);

                    friendStreakDescription.setText("No streak yet!");
                } else {
                    // Hide "no habits" message
                    tvNoHabitsMessage.setVisibility(View.GONE);

                    // Sort habits by streak count (descending)
                    Collections.sort(trackedHabits, new Comparator<Habit>() {
                        @Override
                        public int compare(Habit h1, Habit h2) {
                            return h2.getStreakCount() - h1.getStreakCount();
                        }
                    });

                    // Update streak description with top habit
                    Habit topHabit = trackedHabits.get(0);
                    if (topHabit.getStreakCount() > 0) {
                        friendStreakDescription.setText("Longest streak: " +
                                topHabit.getStreakCount() + " days of " +
                                topHabit.getHabitName() + "!");
                    } else {
                        friendStreakDescription.setText("No streak yet!");
                    }

                    // Update top 3 habits
                    setTopHabitSlot(0, trackedHabits);
                    setTopHabitSlot(1, trackedHabits);
                    setTopHabitSlot(2, trackedHabits);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FriendProfileActivity.this,
                        "Error loading habits", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setTopHabitSlot(int index, List<Habit> sortedHabits) {
        ImageView imageView;
        TextView textView;

        // Determine which views to use based on index
        if (index == 0) {
            imageView = ivFriendTopHabit1;
            textView = tvFriendTopHabit1;
        } else if (index == 1) {
            imageView = ivFriendTopHabit2;
            textView = tvFriendTopHabit2;
        } else {
            imageView = ivFriendTopHabit3;
            textView = tvFriendTopHabit3;
        }

        // If index is out of bounds, hide the views
        if (index >= sortedHabits.size()) {
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            return;
        }

        // Otherwise, show the habit info
        imageView.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);

        Habit habit = sortedHabits.get(index);

        // Set the habit icon based on habit name
        int iconResId;
        String habitName = habit.getHabitName();

        // Match habit name to the correct icon
        if (habitName.contains("water")) {
            iconResId = R.drawable.ic_water;
        } else if (habitName.contains("Workout") || habitName.contains("workout")) {
            iconResId = R.drawable.ic_workout;
        } else if (habitName.contains("homework")) {
            iconResId = R.drawable.ic_homework;
        } else if (habitName.contains("book") || habitName.contains("Read")) {
            iconResId = R.drawable.ic_book;
        } else if (habitName.contains("Meditate") || habitName.contains("meditate")) {
            iconResId = R.drawable.ic_meditation;
        } else if (habitName.contains("money") || habitName.contains("Save")) {
            iconResId = R.drawable.ic_savemoney;
        } else if (habitName.contains("vegetable")) {
            iconResId = R.drawable.ic_vegetable;
        } else if (habitName.contains("phone")) {
            iconResId = R.drawable.ic_phonebanned;
        } else if (habitName.contains("walk") || habitName.contains("Walk")) {
            iconResId = R.drawable.ic_walk_icon;
        } else if (habitName.contains("tea") || habitName.contains("Tea")) {
            iconResId = R.drawable.ic_tea;
        } else if (habitName.contains("Compliment") || habitName.contains("compliment")) {
            iconResId = R.drawable.ic_compliment;
        } else if (habitName.contains("Journal") || habitName.contains("journal")) {
            iconResId = R.drawable.ic_journal;
        } else if (habitName.contains("social media") || habitName.contains("Social Media")) {
            iconResId = R.drawable.ic_nosocial;
        } else if (habitName.contains("Stretch") || habitName.contains("stretch")) {
            iconResId = R.drawable.ic_stretch;
        } else if (habitName.contains("Sleep") || habitName.contains("sleep")) {
            iconResId = R.drawable.ic_sleep;
        } else {
            // Default icon for unrecognized habits
            iconResId = R.drawable.ic_workout; // Use any available icon as default
        }

        // Set the icon resource
        imageView.setImageResource(iconResId);

        // Set the streak text
        textView.setText("🔥 " + habit.getStreakCount() + " days");
    }

    public static void openProfile(Context context, String friendKey) {
        Intent intent = new Intent(context, FriendProfileActivity.class);
        intent.putExtra(EXTRA_FRIEND_KEY, friendKey);
        context.startActivity(intent);
    }
}