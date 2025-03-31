package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.northeastern.guildly.adapters.ChatListAdapter;
import edu.northeastern.guildly.data.Chats;
import edu.northeastern.guildly.data.FriendChatItem;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.data.User;
import edu.northeastern.guildly.data.FriendChatItem;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<FriendChatItem> friendChatList; // each row = one friend

    private DatabaseReference usersRef, chatsRef;
    private String myUserKey; // e.g., "alice@example,com"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerViewChatList = findViewById(R.id.recyclerViewChatList);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));

        friendChatList = new ArrayList<>();

        // On friend row click
        chatListAdapter = new ChatListAdapter(friendChatList, item -> {
            if (item.chatId != null) {
                // Chat already exists
                openChatDetail(item.chatId);
            } else {
                // No chat => create one
                createNewChat(item.friendKey, item.friendUsername);
            }
        });
        recyclerViewChatList.setAdapter(chatListAdapter);

        // Suppose user is logged in
        String myEmail = MainActivity.currentUserEmail;
        if (myEmail == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        myUserKey = myEmail.replace(".", ",");

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        loadAllMyFriends();
    }

    /**
     * 1) Load my friend keys from /users/<myUserKey>/friends
     * 2) For each friendKey -> fetch friend’s username
     * 3) Check if a chat with friend exists -> get last message or placeholder
     * 4) Build friendChatList for the adapter
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
                                if (friendKey != null) {
                                    friendKeys.add(friendKey);
                                }
                            }
                            // Now fetch friend data in parallel
                            fetchFriendData(friendKeys);
                        } else {
                            // No friends
                            chatListAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChatListActivity", "loadAllMyFriends cancelled", error.toException());
                    }
                });
    }

    private void fetchFriendData(List<String> friendKeys) {
        if (friendKeys.isEmpty()) {
            chatListAdapter.notifyDataSetChanged();
            return;
        }

        for (String friendKey : friendKeys) {
            // fetch friend’s username from /users/<friendKey>/username
            usersRef.child(friendKey).child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String friendUsername = snapshot.getValue(String.class);
                            if (friendUsername == null) {
                                friendUsername = friendKey; // fallback
                            }
                            // Now check if there's an existing chat
                            findExistingChat(friendKey, friendUsername);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("ChatListActivity", "fetchFriendData cancelled", error.toException());
                        }
                    });
        }
    }

    /**
     * Query /chats to find a conversation that has participants = [myUserKey, friendKey].
     * If found, get last message. Otherwise, "Say hello to <friendUsername>"
     */
    private void findExistingChat(String friendKey, String friendUsername) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingChatId = null;
                String lastMessageText = "Say hello to " + friendUsername;
                int lastMsgIcon = -1; // means no icon

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    Chats chatObj = chatSnap.getValue(Chats.class);
                    if (chatObj == null || chatObj.participants == null) continue;

                    if (chatObj.participants.size() == 2
                            && chatObj.participants.contains(myUserKey)
                            && chatObj.participants.contains(friendKey)) {

                        // found existing chat
                        existingChatId = chatObj.chatId;

                        if (chatObj.messages != null && !chatObj.messages.isEmpty()) {
                            // get last message
                            Message lastMsg = findLastMessage(chatObj);
                            if (lastMsg != null) {
                                lastMessageText = lastMsg.content;

                                // if lastMsg.senderId == myUserKey => you sent it
                                // then check lastMsg.status
                                if (lastMsg.senderId.equals(myUserKey)) {
                                    // Suppose "SENT" -> ic_msg_solid, "READ" -> ic_msg_hollow
                                    if ("SENT".equals(lastMsg.status)) {
                                        lastMsgIcon = R.drawable.ic_msg_solid;
                                    } else if ("READ".equals(lastMsg.status)) {
                                        lastMsgIcon = R.drawable.ic_msg_hollow;
                                    }
                                } else {
                                    // They sent the last message
                                    // If "SENT" or "READ" might mean they or you read it?
                                    // For example, you can interpret "SENT" => you haven't opened?
                                    if ("SENT".equals(lastMsg.status)) {
                                        // Maybe show a "solid" to indicate you haven't opened it
                                        lastMsgIcon = R.drawable.ic_msg_solid;
                                    } else if ("READ".equals(lastMsg.status)) {
                                        // maybe a "hollow" means you opened it but didn't respond
                                        lastMsgIcon = R.drawable.ic_msg_hollow;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }

                // build the FriendChatItem
                FriendChatItem item = new FriendChatItem(
                        friendKey,
                        friendUsername,
                        existingChatId,
                        lastMessageText,
                        lastMsgIcon
                );
                friendChatList.add(item);
                chatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatListActivity", "findExistingChat cancelled", error.toException());
            }
        });
    }

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


    private String getLastMessage(Chats chatObj) {
        // find the message with the largest timestamp
        long maxTime = -1;
        String content = "";
        for (Message msg : chatObj.messages.values()) {
            if (msg.timestamp > maxTime) {
                maxTime = msg.timestamp;
                content = msg.content;
            }
        }
        return content;
    }

    private void openChatDetail(String chatId) {
        // Launch ChatDetailActivity with chatId
        Intent intent = new Intent(ChatListActivity.this, ChatDetailActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        startActivity(intent);
    }

    /**
     * If no existing chat, create a new one in /chats.
     * participants = [myUserKey, friendKey],
     * chatId = push key
     */
    private void createNewChat(String friendKey, String friendUsername) {
        // push new chat
        String newChatId = chatsRef.push().getKey();
        if (newChatId == null) {
            Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build minimal Chats object
        Chats newChat = new Chats();
        newChat.chatId = newChatId;
        List<String> parts = new ArrayList<>();
        parts.add(myUserKey);
        parts.add(friendKey);
        newChat.participants = parts;
        // messages = null or empty

        chatsRef.child(newChatId).setValue(newChat)
                .addOnSuccessListener(aVoid -> {
                    // Now open ChatDetailActivity
                    openChatDetail(newChatId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatListActivity", "createNewChat failed", e);
                    Toast.makeText(ChatListActivity.this,
                            "Failed to create chat: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
