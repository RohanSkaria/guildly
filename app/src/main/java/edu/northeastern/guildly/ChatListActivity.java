package edu.northeastern.guildly;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.northeastern.guildly.adapters.ChatListAdapter;
import edu.northeastern.guildly.data.Chats;
import edu.northeastern.guildly.data.FriendChatItem;
import edu.northeastern.guildly.data.Message;

public class ChatListActivity extends AppCompatActivity {
    private static final String TAG = "ChatListActivity";

    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<FriendChatItem> friendChatList;

    private DatabaseReference usersRef, chatsRef;
    private String myUserKey;
    private ImageButton backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerViewChatList = findViewById(R.id.recyclerViewChatList);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));

        backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener(v -> finish());

        friendChatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(friendChatList, item -> {
            if (item.chatId != null) {
                // If an existing chatId, go to detail
                ChatDetailActivity.openChatDetail(
                        ChatListActivity.this,
                        item.chatId,
                        item.friendUsername
                );
            } else {
                // Otherwise create a new chat
                createNewChat(item.friendKey, item.friendUsername);
            }
        });
        recyclerViewChatList.setAdapter(chatListAdapter);

        // Get current user key
        String myEmail = MainActivity.currentUserEmail;
        if (myEmail == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        myUserKey = myEmail.replace(".", ",");

        // Firebase references
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        loadAllMyFriends();
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatListAdapter.notifyDataSetChanged();
    }

    /**
     * Load the friend list of the current user.
     */
    private void loadAllMyFriends() {
        usersRef.child(myUserKey).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        friendChatList.clear();
                        if (snapshot.exists()) {
                            List<String> friendKeys = new ArrayList<>();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String friendKey = ds.getValue(String.class);
                                // --- IMPORTANT NULL CHECK ---
                                if (friendKey != null && !friendKey.trim().isEmpty()) {
                                    friendKeys.add(friendKey);
                                } else {
                                    Log.e(TAG, "Skipped a null/empty friendKey");
                                }
                            }
                            fetchFriendData(friendKeys);
                        } else {
                            chatListAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadAllMyFriends cancelled", error.toException());
                    }
                });
    }

    /**
     * For each friendKey, fetch that friend's username.
     */
    private void fetchFriendData(List<String> friendKeys) {
        if (friendKeys.isEmpty()) {
            chatListAdapter.notifyDataSetChanged();
            return;
        }

        for (String friendKey : friendKeys) {
            // If friendKey is null, skip it (extra protection)
            if (friendKey == null) {
                Log.e(TAG, "friendKey was null. Skipping...");
                continue;
            }

            usersRef.child(friendKey).child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String friendUsername = snapshot.getValue(String.class);

                            // If username was never set, default to friendKey
                            if (friendUsername == null || friendUsername.trim().isEmpty()) {
                                friendUsername = friendKey;
                            }
                            // Now find if there's an existing chat between me & friend
                            findExistingChat(friendKey, friendUsername);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "fetchFriendData cancelled", error.toException());
                        }
                    });
        }
    }

    /**
     * Look through all chats to find one that has exactly these two participants:
     * myUserKey and friendKey. If found, use that chatId; else it's null.
     */
    private void findExistingChat(String friendKey, String friendUsername) {
        if (friendKey == null) {
            // Double check at runtime
            Log.e(TAG, "findExistingChat called with null friendKey?");
            return;
        }

        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingChatId = null;
                String lastMessageText = "Say hello to " + friendUsername;
                int lastMsgIcon = -1;
                String timestamp = "";

                // Loop all chats
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    Chats chatObj = chatSnap.getValue(Chats.class);
                    if (chatObj == null || chatObj.participants == null) {
                        continue;
                    }
                    // If chat has exactly 2 participants: me & friend
                    if (chatObj.participants.size() == 2
                            && chatObj.participants.contains(myUserKey)
                            && chatObj.participants.contains(friendKey)) {

                        existingChatId = chatObj.chatId;
                        // Find the last message (if any)
                        if (chatObj.messages != null && !chatObj.messages.isEmpty()) {
                            Message lastMsg = findLastMessage(chatObj);
                            if (lastMsg != null) {
                                lastMessageText = lastMsg.content;
                                timestamp = formatTimestamp(lastMsg.timestamp);
                                // Example logic for icons
                                if (lastMsg.senderId.equals(myUserKey)) {
                                    if ("SENT".equals(lastMsg.status)) {
                                        lastMsgIcon = R.drawable.ic_msg_solid;
                                    } else if ("READ".equals(lastMsg.status)) {
                                        lastMsgIcon = R.drawable.ic_msg_hollow;
                                    }
                                } else {
                                    if ("SENT".equals(lastMsg.status)) {
                                        lastMsgIcon = R.drawable.ic_msg_solid;
                                    } else if ("READ".equals(lastMsg.status)) {
                                        lastMsgIcon = R.drawable.ic_msg_hollow;
                                    }
                                }
                            }
                        }
                        // Once we find a matching chat, break out
                        break;
                    }
                }

                // Create the FriendChatItem (only if friendKey != null)
                FriendChatItem item = new FriendChatItem(
                        friendKey,
                        friendUsername,
                        existingChatId,
                        lastMessageText,
                        lastMsgIcon,
                        timestamp
                );
                friendChatList.add(item);
                chatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "findExistingChat cancelled", error.toException());
            }
        });
    }

    /**
     * Utility: find the message with the largest timestamp in the chat.
     */
    private Message findLastMessage(Chats chatObj) {
        long maxTime = -1;
        Message latestMsg = null;
        for (Message msg : chatObj.messages.values()) {
            if (msg.timestamp > maxTime) {
                maxTime = msg.timestamp;
                latestMsg = msg;
            }
        }
        return latestMsg;
    }

    /**
     * Convert a millisecond timestamp to "hh:mm a" format (e.g. "01:34 PM")
     */
    private String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    /**
     * Create a new chat (if none exists) and jump into ChatDetailActivity.
     * Fixed to avoid overwriting the habits field.
     */
    private void createNewChat(String friendKey, String friendUsername) {
        if (friendKey == null) {
            Toast.makeText(this,
                    "Cannot start chat: friend key is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a new chat ID
        String newChatId = chatsRef.push().getKey();
        if (newChatId == null) {
            Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create participants list
        List<String> participants = new ArrayList<>();
        participants.add(myUserKey);
        participants.add(friendKey);

        // Create a chat object with only the necessary fields
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", newChatId);
        chatData.put("participants", participants);

        // Directly save the chat object with specific fields
        chatsRef.child(newChatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    // After success, open the chat
                    ChatDetailActivity.openChatDetail(
                            ChatListActivity.this,
                            newChatId,
                            friendUsername
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createNewChat failed", e);
                    Toast.makeText(ChatListActivity.this,
                            "Failed to create chat: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}