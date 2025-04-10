package edu.northeastern.guildly;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.Habit;

/**
 * Manages the weekly challenge in Firebase.
 */
public class WeeklyChallengeManager {

    // Reference to the "challenges/currentWeeklyChallenge" node in Firebase
    private final DatabaseReference weeklyChallengeRef;

    // A list of possible weekly challenges to pick from
    private final List<Habit> weeklyChallengeOptions = Arrays.asList(
            new Habit("Take a walk outside", R.drawable.ic_walk_icon),
            new Habit("Drink tea instead of coffee", R.drawable.ic_tea),
            new Habit("Compliment someone", R.drawable.ic_compliment),
            new Habit("Journal for 5 minutes", R.drawable.ic_journal),
            new Habit("No social media today", R.drawable.ic_nosocial),
            new Habit("Stretch for 10 minutes", R.drawable.ic_stretch),
            new Habit("Sleep 8+ hours", R.drawable.ic_sleep)
    );

    public WeeklyChallengeManager() {
        // Point to "/challenges/currentWeeklyChallenge" in your Firebase DB
        weeklyChallengeRef = FirebaseDatabase.getInstance()
                .getReference("challenges")
                .child("currentWeeklyChallenge");
    }

    /**
     * Checks if the current weekly challenge is missing or has expired.
     * If so, picks a new random one from weeklyChallengeOptions and updates Firebase.
     * If it's valid (not expired), does nothing.
     *
     * @param onComplete Callback to run after we've checked and (possibly) updated the challenge.
     */
    public void checkAndUpdateWeeklyChallenge(@NonNull Runnable onComplete) {
        weeklyChallengeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long endTimeMillis = snapshot.child("endTimeMillis").getValue(Long.class);
                long now = System.currentTimeMillis();

                // If endTimeMillis is null or the current time is beyond the end time, pick a new challenge
                if (endTimeMillis == null || now > endTimeMillis) {
                    // current weekly challenge is missing or expired
                    Habit newChallenge = pickRandomChallenge();
                    long oneWeekFromNow = now + (7L * 24 * 60 * 60 * 1000); // 7 days in milliseconds

                    Map<String, Object> challengeMap = new HashMap<>();
                    challengeMap.put("habitName", newChallenge.getHabitName());
                    challengeMap.put("iconResId", newChallenge.getIconResId());
                    challengeMap.put("startTimeMillis", now);
                    challengeMap.put("endTimeMillis", oneWeekFromNow);

                    // Write the new challenge to Firebase
                    weeklyChallengeRef.updateChildren(challengeMap)
                            .addOnCompleteListener(task -> onComplete.run());
                } else {
                    // It's still valid, do nothing special
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onComplete.run();
            }
        });
    }

    /**
     * Reads the current weekly challenge from Firebase (even if it's not expired),
     * so you can display/update it in your UI.
     *
     * @param listener A ValueEventListener to receive the snapshot with "habitName", "iconResId", etc.
     */
    public void loadWeeklyChallenge(@NonNull ValueEventListener listener) {
        weeklyChallengeRef.addListenerForSingleValueEvent(listener);
    }

    /**
     * Picks a random challenge from the weeklyChallengeOptions list.
     */
    private Habit pickRandomChallenge() {
        int randomIndex = (int) (Math.random() * weeklyChallengeOptions.size());
        return weeklyChallengeOptions.get(randomIndex);
    }
}
