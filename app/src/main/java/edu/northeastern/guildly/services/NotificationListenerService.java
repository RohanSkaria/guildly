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

import java.util.Map;

import edu.northeastern.guildly.MainActivity;
import edu.northeastern.guildly.data.Message;

public class NotificationListenerService extends Service {

    private static final String TAG = "NotifListenerService";

    private DatabaseReference notificationsRef;
    private DatabaseReference friendRequestsRef;
    private ChildEventListener notificationsListener;
    private ChildEventListener friendRequestsListener;
    private String userKey;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationService.createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String email = MainActivity.currentUserEmail;
        if (email != null) {
            userKey = email.replace(".", ",");
            setupNotificationListener();
            setupFriendRequestListener();
            setupMessageListener();
        } else {
            Log.e(TAG, "Cannot start notification service: no logged-in user");
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        detachNotificationListener();
        detachFriendRequestListener();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupNotificationListener() {
        detachNotificationListener();
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userKey);
        notificationsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                handleNewNotification(snapshot);
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Notification listener cancelled", error.toException());
            }
        };
        notificationsRef.addChildEventListener(notificationsListener);
    }

    private void setupFriendRequestListener() {
        detachFriendRequestListener();
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("users").child(userKey).child("friendRequests");
        friendRequestsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String requesterKey = snapshot.getKey();
                String status = snapshot.getValue(String.class);
                if ("pending".equals(status)) {
                    FirebaseDatabase.getInstance().getReference("users").child(requesterKey)
                            .child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String username = dataSnapshot.getValue(String.class);
                                    if (username != null) {
                                        NotificationService.showFriendRequestNotification(getApplicationContext(), username);
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "FriendRequest listener cancelled", error.toException());
            }
        };
        friendRequestsRef.addChildEventListener(friendRequestsListener);
    }

    private void detachNotificationListener() {
        if (notificationsRef != null && notificationsListener != null) {
            notificationsRef.removeEventListener(notificationsListener);
            notificationsListener = null;
        }
    }

    private void detachFriendRequestListener() {
        if (friendRequestsRef != null && friendRequestsListener != null) {
            friendRequestsRef.removeEventListener(friendRequestsListener);
            friendRequestsListener = null;
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

    private void setupMessageListener() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String chatId = chatSnap.getKey();
                    DataSnapshot participantsSnap = chatSnap.child("participants");

                    boolean isParticipant = false;
                    for (DataSnapshot p : participantsSnap.getChildren()) {
                        String email = p.getValue(String.class);
                        if (email != null && userKey.equals(email.replace(".", ","))) {
                            isParticipant = true;
                            break;
                        }
                    }

                    if (isParticipant) {
                        DatabaseReference messagesRef = chatsRef.child(chatId).child("messages");
                        messagesRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                Message msg = snapshot.getValue(Message.class);
                                if (msg != null && !msg.senderId.equals(userKey)) {
                                    FirebaseDatabase.getInstance().getReference("users")
                                            .child(msg.senderId)
                                            .child("username")
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    String senderUsername = "Someone";
                                                    Object val = snapshot.getValue();
                                                    if (val instanceof String) {
                                                        senderUsername = (String) val;
                                                    } else if (val instanceof Map) {
                                                        Map<String, Object> map = (Map<String, Object>) val;
                                                        senderUsername = map.getOrDefault("first", "") + " " + map.getOrDefault("last", "");
                                                    }
                                                    NotificationService.showNewMessageNotification(getApplicationContext(), senderUsername, msg.content);
                                                }

                                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                                            });
                                }
                            }
                            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
