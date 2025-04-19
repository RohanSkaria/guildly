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
import java.util.Random;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.Habit;

/**
 * Enhanced WeeklyChallengeManager that:
 * 1. Manages the global weekly challenge
 * 2. Tracks user progress with multiple completions per week (random required count)
 * 3. Enforces 24-hour cooldown between completions
 * 4. Maintains streak count for consecutive weekly completions
 */
public class WeeklyChallengeManager {

    private static final String TAG = "WeeklyChallengeManager";

    // ------------------------------------------------------------------------
    // GLOBAL CHALLENGE FIELDS
    // ------------------------------------------------------------------------

    /** Reference to the global "currentWeeklyChallenge" node in Firebase. */
    private final DatabaseReference weeklyChallengeRef;

    /** The list of possible weekly challenges to choose from. */
    private final List<Habit> weeklyChallengeOptions = Arrays.asList(
            new Habit("Take a walk outside", R.drawable.ic_walk_icon),
            new Habit("Drink tea instead of coffee", R.drawable.ic_tea),
            new Habit("Compliment someone", R.drawable.ic_compliment),
            new Habit("Journal for 5 minutes", R.drawable.ic_journal),
            new Habit("No social media today", R.drawable.ic_nosocial),
            new Habit("Stretch for 10 minutes", R.drawable.ic_stretch),
            new Habit("Sleep 8+ hours", R.drawable.ic_sleep)
    );

    // ------------------------------------------------------------------------
    // USER-SPECIFIC FIELDS
    // ------------------------------------------------------------------------

    /** This userKey is the sanitized version of the user's email, e.g. "user@example,com". */
    private final String userKey;

    /** Reference to the per-user "weeklyChallengeProgress" node in Firebase. */
    private final DatabaseReference userChallengeProgressRef;

    /** The minimum number of completions for weekly challenge */
    private static final int MIN_REQUIRED_COMPLETIONS = 3;

    /** The maximum number of completions for weekly challenge */
    private static final int MAX_REQUIRED_COMPLETIONS = 7;

    // ------------------------------------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------------------------------------

    /**
     * Constructs the WeeklyChallengeManager for a given user.
     *
     * @param userEmail The logged-in user's email address.
     */
    public WeeklyChallengeManager(@NonNull String userEmail) {
        // 1) A reference to the global weekly challenge node: "/challenges/currentWeeklyChallenge"
        weeklyChallengeRef = FirebaseDatabase.getInstance()
                .getReference("challenges")
                .child("currentWeeklyChallenge");

        // 2) A reference to the user's progress node: "/users/<userKey>/weeklyChallengeProgress"
        this.userKey = userEmail.replace(".", ",");
        userChallengeProgressRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userKey)
                .child("weeklyChallengeProgress");
    }

    // ------------------------------------------------------------------------
    // MAIN METHODS
    // ------------------------------------------------------------------------

    /**
     * Checks whether the global weekly challenge is missing or expired.
     * - If missing/expired, picks a new random challenge, resets the global node,
     *   and resets the user's progress with a random completion target.
     * - Otherwise, if it still exists, ensures the user is in sync.
     *
     * @param onComplete A callback that fires once the check is done.
     */
    public void checkAndUpdateWeeklyChallenge(@NonNull Runnable onComplete) {
        weeklyChallengeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Read the global challenge's end time
                Long endTimeMillis = snapshot.child("endTimeMillis").getValue(Long.class);
                long now = System.currentTimeMillis();

                // If no challenge is posted or it's expired (i.e. current time > end time)
                if (endTimeMillis == null || now > endTimeMillis) {
                    // 1) Pick a new challenge from weeklyChallengeOptions
                    Habit newChallenge = pickRandomChallenge();
                    long oneWeekFromNow = now + (7L * 24 * 60 * 60 * 1000); // 7 days in milliseconds

                    // 2) Build a map to set the new challenge fields
                    Map<String, Object> newChallengeMap = new HashMap<>();
                    newChallengeMap.put("habitName", newChallenge.getHabitName());
                    newChallengeMap.put("iconResId", newChallenge.getIconResId());
                    newChallengeMap.put("startTimeMillis", now);
                    newChallengeMap.put("endTimeMillis", oneWeekFromNow);

                    // 3) Update the global weekly challenge node in Firebase
                    weeklyChallengeRef.updateChildren(newChallengeMap)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // 4) Reset the user's progress for the new challenge
                                    // with a random required completion count
                                    resetUserProgress(now, generateRandomRequiredCompletions(), () -> onComplete.run());
                                } else {
                                    onComplete.run();
                                }
                            });
                } else {
                    // There's an active challenge. We check its startTime to see if the user is aligned.
                    Long startTimeMillis = snapshot.child("startTimeMillis").getValue(Long.class);
                    if (startTimeMillis == null) startTimeMillis = 0L;

                    // Sync the user with the current challenge's start time
                    syncUserWithCurrentChallengeIfNeeded(startTimeMillis, () -> onComplete.run());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onComplete.run();
            }
        });
    }

    /**
     * Updates the weeklyChallengePts counter when a user fully completes a weekly challenge.
     * This is a lifetime counter of completed weekly challenges (not tied to streaks).
     *
     * @param isCompleted Whether the user has completed all required completions
     */
    private void updateWeeklyChallengePoints(boolean isCompleted) {
        if (!isCompleted) return; // Only increment if completed

        // Reference to the user's weeklyChallengePts
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userKey);

        userRef.child("weeklyChallengePts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long currentPoints = snapshot.getValue(Long.class);
                long newPoints = (currentPoints != null ? currentPoints : 0) + 1;

                userRef.child("weeklyChallengePts").setValue(newPoints);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently
            }
        });
    }

    /**
     * Reads the current global weekly challenge from Firebase (e.g., habitName, iconResId, etc.).
     * You can then display these values in your UI (like in HomeFragment).
     *
     * @param listener The ValueEventListener that handles the data snapshot.
     */
    public void loadWeeklyChallenge(@NonNull ValueEventListener listener) {
        weeklyChallengeRef.addListenerForSingleValueEvent(listener);
    }

    /**
     * Attempts to "complete" the weekly challenge for the current user, enforcing:
     *  1) The global challenge must not be expired.
     *  2) The user must wait for the 24-hour cooldown (nextAvailableTime).
     *  3) The user can only complete up to the required number of times per week.
     *
     * The result is given to 'callback.onResult(...)' as a String message that
     * can be displayed in a Toast or anywhere in your UI.
     *
     * @param callback A callback interface to handle the result message.
     */
    public void attemptWeeklyChallengeCompletion(@NonNull ChallengeCompletionCallback callback) {
        // 1) Check the global challenge to see if it's still active
        weeklyChallengeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot globalSnap) {
                Long endTimeMillis = globalSnap.child("endTimeMillis").getValue(Long.class);
                Long startTimeMillis = globalSnap.child("startTimeMillis").getValue(Long.class);
                long now = System.currentTimeMillis();

                // If there's no valid challenge or start/end time, we can't proceed
                if (endTimeMillis == null || startTimeMillis == null) {
                    callback.onResult("No current weekly challenge found. Please try again later.");
                    return;
                }
                // If the challenge is expired (current time > end time), user can't do it
                if (now > endTimeMillis) {
                    callback.onResult("This week's challenge has already ended! Please wait for next week's challenge.");
                    return;
                }

                // 2) If the global challenge is valid, check the user's progress
                userChallengeProgressRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        // Pull the user's nextAvailableTime, fullyCompleted, and completedCount
                        long nextAvailableTime = userSnap.child("nextAvailableTime").getValue(Long.class) != null
                                ? userSnap.child("nextAvailableTime").getValue(Long.class) : 0L;
                        boolean fullyCompleted = userSnap.child("fullyCompleted").getValue(Boolean.class) != null
                                && userSnap.child("fullyCompleted").getValue(Boolean.class);
                        long completedCount = userSnap.child("completedCountThisWeek").getValue(Long.class) != null
                                ? userSnap.child("completedCountThisWeek").getValue(Long.class) : 0L;

                        // Get the required completions for this week
                        long requiredCompletions = userSnap.child("requiredCompletions").getValue(Long.class) != null
                                ? userSnap.child("requiredCompletions").getValue(Long.class)
                                : MIN_REQUIRED_COMPLETIONS;

                        // 2a) If the user already did enough completions (or fullyCompleted is true)
                        if (fullyCompleted || completedCount >= requiredCompletions) {
                            callback.onResult("You've already completed this week's challenge! Come back for next week's challenge.");
                            return;
                        }

                        // 2b) If the user is still on a 24-hour cooldown
                        if (now < nextAvailableTime) {
                            long remaining = nextAvailableTime - now;
                            long hours = remaining / (1000 * 60 * 60);
                            long mins = (remaining / (1000 * 60)) % 60;

                            String msg = String.format(
                                    "You must wait %d hours and %d minutes before doing the weekly challenge again.",
                                    hours, mins
                            );
                            callback.onResult(msg);
                            return;
                        }

                        // 3) If none of the above conditions block them, they can complete the challenge now
                        long newCount = completedCount + 1;           // increment the user's completion count
                        long oneDay = 24L * 60L * 60L * 1000L;        // 24 hours in milliseconds
                        long newNextAvailableTime = now + oneDay;     // user can do it again after 24 hours

                        // Build a map of updates to write to Firebase
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("completedCountThisWeek", newCount);
                        updates.put("nextAvailableTime", newNextAvailableTime);

                        // If the user just hit or exceeded the required completions, mark them fully completed
                        if (newCount >= requiredCompletions) {
                            updates.put("fullyCompleted", true);
                            updateWeeklyChallengePoints(true);
                        }

                        // 4) Write the updates back to the user's progress node
                        userChallengeProgressRef.updateChildren(updates)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        // If any error occurred writing to the DB
                                        callback.onResult("Error updating your progress. Please try again.");
                                        return;
                                    }

                                    // 5) Successfully updated. Compose a success message:
                                    if (newCount >= requiredCompletions) {
                                        // The user has now fully completed the challenge for this week
                                        callback.onResult("Congratulations! You've completed the weekly challenge. See you next week!");
                                    } else {
                                        // Show how many completions they have done so far
                                        callback.onResult(
                                                "Weekly challenge completed for today! You have done "
                                                        + newCount + " of "
                                                        + requiredCompletions
                                                        + " completions this week."
                                        );
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // If something went wrong reading the user's node
                        callback.onResult("Error loading user progress: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If something went wrong reading the global node
                callback.onResult("Error loading global challenge: " + error.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------------
    // HELPER METHODS
    // ------------------------------------------------------------------------

    /**
     * Picks a random weekly challenge from the predefined list.
     */
    private Habit pickRandomChallenge() {
        int randomIndex = (int) (Math.random() * weeklyChallengeOptions.size());
        return weeklyChallengeOptions.get(randomIndex);
    }

    /**
     * Generates a random number of required completions between MIN_REQUIRED_COMPLETIONS
     * and MAX_REQUIRED_COMPLETIONS.
     */
    private int generateRandomRequiredCompletions() {
        Random random = new Random();
        return random.nextInt(MAX_REQUIRED_COMPLETIONS - MIN_REQUIRED_COMPLETIONS + 1) + MIN_REQUIRED_COMPLETIONS;
    }

    /**
     * Resets the user's progress node for a new challenge that starts at 'newChallengeStart'.
     * Also sets a random number of required completions.
     */
    private void resetUserProgress(long newChallengeStart, int requiredCompletions, Runnable onDone) {
        Map<String, Object> resetMap = new HashMap<>();
        resetMap.put("completedCountThisWeek", 0);
        resetMap.put("nextAvailableTime", 0);
        resetMap.put("fullyCompleted", false);
        resetMap.put("challengeStartTime", newChallengeStart);
        resetMap.put("requiredCompletions", requiredCompletions);

        // Check if we need to increment or reset streak
        checkAndUpdateStreak(lastWeekCompleted -> {
            if (lastWeekCompleted) {
                // Increment the streak if the user completed last week's challenge
                userChallengeProgressRef.child("streakCount").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentStreak = snapshot.getValue(Long.class);
                        if (currentStreak == null) currentStreak = 0L;
                        resetMap.put("streakCount", currentStreak + 1);

                        // Finalize the update
                        userChallengeProgressRef.updateChildren(resetMap)
                                .addOnCompleteListener(task -> onDone.run());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        resetMap.put("streakCount", 1); // Default if error
                        userChallengeProgressRef.updateChildren(resetMap)
                                .addOnCompleteListener(task -> onDone.run());
                    }
                });
            } else {
                // Reset the streak to 0 if the user didn't complete last week's challenge
                resetMap.put("streakCount", 0);
                userChallengeProgressRef.updateChildren(resetMap)
                        .addOnCompleteListener(task -> onDone.run());
            }
        });
    }

    /**
     * Check if the user completed last week's challenge to determine streak
     * continuation.
     */
    private void checkAndUpdateStreak(StreakCheckCallback callback) {
        userChallengeProgressRef.child("fullyCompleted").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean completed = snapshot.getValue(Boolean.class);
                callback.onResult(completed != null && completed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onResult(false); // Default to false if error
            }
        });
    }

    /**
     * Checks if the user is aligned with the current challenge start time.
     * If not, resets their progress to match the new challenge.
     */
    private void syncUserWithCurrentChallengeIfNeeded(long globalStartTime, Runnable onDone) {
        userChallengeProgressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long userStartTime = snapshot.child("challengeStartTime").getValue(Long.class);
                if (userStartTime == null) userStartTime = 0L;

                // If the user's challengeStartTime doesn't match the global one, reset
                if (!userStartTime.equals(globalStartTime)) {
                    // Get or generate required completions
                    Long requiredCompletions = snapshot.child("requiredCompletions").getValue(Long.class);
                    int newRequiredCompletions = (requiredCompletions != null)
                            ? requiredCompletions.intValue()
                            : generateRandomRequiredCompletions();

                    resetUserProgress(globalStartTime, newRequiredCompletions, onDone);
                } else {
                    onDone.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onDone.run();
            }
        });
    }

    // ------------------------------------------------------------------------
    // CALLBACK INTERFACES
    // ------------------------------------------------------------------------

    /**
     * Callback interface so the UI (e.g., your HomeFragment) can receive a
     * result message and show it (like via a Toast or dialog).
     */
    public interface ChallengeCompletionCallback {
        void onResult(String message);
    }

    /**
     * Callback for checking if the previous week's challenge was completed.
     */
    private interface StreakCheckCallback {
        void onResult(boolean lastWeekCompleted);
    }
}