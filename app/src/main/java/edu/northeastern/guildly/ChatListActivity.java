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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.northeastern.guildly.adapters.ChatListAdapter;
import edu.northeastern.guildly.data.Chats;
import edu.northeastern.guildly.data.FriendChatItem;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.data.User;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<FriendChatItem> friendChatList;

    private DatabaseReference usersRef, chatsRef;
    private String myUserKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerViewChatList = findViewById(R.id.recyclerViewChatList);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));

        friendChatList = new ArrayList<>();

        chatListAdapter = new ChatListAdapter(friendChatList, item -> {
            if (item.chatId != null) {
                ChatDetailActivity.openChatDetail(ChatListActivity.this, item.chatId, item.friendUsername);
            } else {
                createNewChat(item.friendKey, item.friendUsername);
            }
        });
        recyclerViewChatList.setAdapter(chatListAdapter);

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

    @Override
    protected void onResume() {
        super.onResume();
        chatListAdapter.notifyDataSetChanged();
    }

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
                            fetchFriendData(friendKeys);
                        } else {
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
            usersRef.child(friendKey).child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String friendUsername = snapshot.getValue(String.class);
                            if (friendUsername == null) {
                                friendUsername = friendKey;
                            }
                            findExistingChat(friendKey, friendUsername);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("ChatListActivity", "fetchFriendData cancelled", error.toException());
                        }
                    });
        }
    }

    private void findExistingChat(String friendKey, String friendUsername) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingChatId = null;
                String lastMessageText = "Say hello to " + friendUsername;
                int lastMsgIcon = -1;
                String timestamp = "";

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    Chats chatObj = chatSnap.getValue(Chats.class);
                    if (chatObj == null || chatObj.participants == null) continue;

                    if (chatObj.participants.size() == 2
                            && chatObj.participants.contains(myUserKey)
                            && chatObj.participants.contains(friendKey)) {

                        existingChatId = chatObj.chatId;

                        if (chatObj.messages != null && !chatObj.messages.isEmpty()) {
                            Message lastMsg = findLastMessage(chatObj);
                            if (lastMsg != null) {
                                lastMessageText = lastMsg.content;
                                timestamp = formatTimestamp(lastMsg.timestamp);

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
                        break;
                    }
                }

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

    private String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private void createNewChat(String friendKey, String friendUsername) {
        String newChatId = chatsRef.push().getKey();
        if (newChatId == null) {
            Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
            return;
        }

        Chats newChat = new Chats();
        newChat.chatId = newChatId;
        List<String> parts = new ArrayList<>();
        parts.add(myUserKey);
        parts.add(friendKey);
        newChat.participants = parts;

        chatsRef.child(newChatId).setValue(newChat)
                .addOnSuccessListener(aVoid -> ChatDetailActivity.openChatDetail(ChatListActivity.this, newChatId, friendUsername))
                .addOnFailureListener(e -> {
                    Log.e("ChatListActivity", "createNewChat failed", e);
                    Toast.makeText(ChatListActivity.this,
                            "Failed to create chat: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}