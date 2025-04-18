package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.northeastern.guildly.adapters.ChatDetailAdapter;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.services.NotificationService;

public class ChatDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChatDetail;
    private ChatDetailAdapter chatDetailAdapter;
    private List<Message> messageList;

    private EditText editTextMessageInput;
    private Button buttonSend;

    private String chatId;
    private String myUserKey;
    private DatabaseReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String friendUsername = getIntent().getStringExtra("FRIEND_USERNAME");
        if (getSupportActionBar() != null && friendUsername != null) {
            getSupportActionBar().setTitle(friendUsername);
        }

        recyclerViewChatDetail = findViewById(R.id.recyclerViewChatDetail);
        editTextMessageInput = findViewById(R.id.editTextMessageInput);
        buttonSend = findViewById(R.id.buttonSend);

        recyclerViewChatDetail.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();

        String myEmail = MainActivity.currentUserEmail;
        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER";

        chatDetailAdapter = new ChatDetailAdapter(messageList, myUserKey);
        recyclerViewChatDetail.setAdapter(chatDetailAdapter);

        chatId = getIntent().getStringExtra("CHAT_ID");
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

        loadMessages();
        setupSendButton();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMessages() {
        DatabaseReference messagesRef = chatRef.child("messages");
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    Message msg = msgSnap.getValue(Message.class);
                    if (msg != null) {
                        messageList.add(msg);
                        if (!msg.senderId.equals(myUserKey) && "SENT".equals(msg.status)) {
                            recyclerViewChatDetail.postDelayed(() -> {
                                msgSnap.getRef().child("status").setValue("READ");
                                NotificationManagerCompat.from(getApplicationContext()).cancel(NotificationService.NOTIFICATION_TYPE_MESSAGE);
                            }, 1000);
                        }
                    }
                }
                Collections.sort(messageList, Comparator.comparingLong(m -> m.timestamp));
                chatDetailAdapter.notifyDataSetChanged();
                recyclerViewChatDetail.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSendButton() {
        buttonSend.setOnClickListener(view -> {
            String text = editTextMessageInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }

            // Get the friend's key from the chat participants
            DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
            chatRef.child("participants").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    final String[] friendKeyHolder = {null};

                    // Find which participant is not the current user
                    for (DataSnapshot participantSnap : snapshot.getChildren()) {
                        String participantKey = participantSnap.getValue(String.class);
                        if (participantKey != null && !participantKey.equals(myUserKey)) {
                            friendKeyHolder[0] = participantKey;
                            break;
                        }
                    }

                    if (friendKeyHolder[0] == null) {
                        Toast.makeText(ChatDetailActivity.this,
                                "Error identifying chat participant", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Store in final variable for inner class access
                    final String friendKey = friendKeyHolder[0];

                    // Check if the friend is in the user's friends list
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("users").child(myUserKey).child("friends");

                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isFriend = false;

                            for (DataSnapshot friendSnap : snapshot.getChildren()) {
                                String currentFriend = friendSnap.getValue(String.class);
                                if (currentFriend != null && currentFriend.equals(friendKey)) {
                                    isFriend = true;
                                    break;
                                }
                            }

                            if (isFriend) {
                                // Proceed with sending the message
                                Message msg = new Message();
                                msg.senderId = myUserKey;
                                msg.content = text;
                                msg.timestamp = System.currentTimeMillis();
                                msg.status = "SENT";

                                DatabaseReference pushRef = chatRef.child("messages").push();
                                pushRef.setValue(msg).addOnSuccessListener(aVoid -> {
                                    chatRef.child("lastUpdated").setValue(System.currentTimeMillis());
                                });

                                editTextMessageInput.setText("");
                            } else {
                                // Not a friend anymore
                                Toast.makeText(ChatDetailActivity.this,
                                        "This user is not your friend. Please add them as a friend to chat.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ChatDetailActivity.this,
                                    "Error checking friend status", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatDetailActivity.this,
                            "Error accessing chat data", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public static String lastOpenedFriendUsername = null;

    public static void openChatDetail(AppCompatActivity activity, String chatId, String friendUsername) {
        lastOpenedFriendUsername = friendUsername;
        Intent intent = new Intent(activity, ChatDetailActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("FRIEND_USERNAME", friendUsername);
        activity.startActivity(intent);
    }
}
