package edu.northeastern.guildly.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import edu.northeastern.guildly.MainActivity;

/**
 * Background service that listens for notifications from Firebase
 */
public class NotificationListenerService extends Service {

    private static final String TAG = "NotifListenerService";
    private DatabaseReference notificationsRef;
    private ChildEventListener notificationsListener;
    private String userKey;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize notification channel for the app
        NotificationService.createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String email = MainActivity.currentUserEmail;
        if (email != null) {
            userKey = email.replace(".", ",");
            setupNotificationListener();
        } else {
            Log.e(TAG, "Cannot start notification service: no logged-in user");
            stopSelf();
        }

        // If killed, restart with the intent
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        detachNotificationListener();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupNotificationListener() {
        detachNotificationListener(); // Detach any existing listener

        notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userKey);

        notificationsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                handleNewNotification(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not needed for this implementation
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Not needed for this implementation
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not needed for this implementation
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Notification listener cancelled", error.toException());
            }
        };

        notificationsRef.addChildEventListener(notificationsListener);
    }

    private void detachNotificationListener() {
        if (notificationsRef != null && notificationsListener != null) {
            notificationsRef.removeEventListener(notificationsListener);
            notificationsListener = null;
        }
    }

    private void handleNewNotification(DataSnapshot snapshot) {
        try {
            String type = snapshot.child("type").getValue(String.class);
            if (type == null) return;

            switch (type) {
                case "friend_request":
                    handleFriendRequestNotification(snapshot);
                    break;
                case "streak_milestone":
                    handleStreakMilestoneNotification(snapshot);
                    break;
                case "weekly_challenge":
                    handleWeeklyChallengeNotification(snapshot);
                    break;
                case "habit_reminder":
                    handleHabitReminderNotification(snapshot);
                    break;
            }

            // Delete the notification from Firebase after processing
            snapshot.getRef().removeValue();

        } catch (Exception e) {
            Log.e(TAG, "Error processing notification", e);
        }
    }

    private void handleFriendRequestNotification(DataSnapshot snapshot) {
        String fromUsername = snapshot.child("fromUsername").getValue(String.class);
        if (fromUsername != null) {
            NotificationService.showFriendRequestNotification(this, fromUsername);
        }
    }

    private void handleStreakMilestoneNotification(DataSnapshot snapshot) {
        String habitName = snapshot.child("habitName").getValue(String.class);
        Long streakCount = snapshot.child("streakCount").getValue(Long.class);

        if (habitName != null && streakCount != null) {
            NotificationService.showStreakMilestoneNotification(this, habitName, streakCount.intValue());
        }
    }

    private void handleWeeklyChallengeNotification(DataSnapshot snapshot) {
        String challengeName = snapshot.child("challengeName").getValue(String.class);

        if (challengeName != null) {
            NotificationService.showWeeklyChallengeNotification(this, challengeName);
        }
    }

    private void handleHabitReminderNotification(DataSnapshot snapshot) {
        String habitName = snapshot.child("habitName").getValue(String.class);

        if (habitName != null) {
            NotificationService.showHabitReminderNotification(this, habitName);
        }
    }
}