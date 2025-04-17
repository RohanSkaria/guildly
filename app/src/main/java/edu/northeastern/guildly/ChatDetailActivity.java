package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

            Message msg = new Message();
            msg.senderId = myUserKey;
            msg.content = text;
            msg.timestamp = System.currentTimeMillis();
            msg.status = "SENT";

            DatabaseReference pushRef = chatRef.child("messages").push();
            pushRef.setValue(msg).addOnSuccessListener(aVoid -> {
                // 👇 Update lastUpdated field when message is sent
                chatRef.child("lastUpdated").setValue(System.currentTimeMillis());
            });

            editTextMessageInput.setText("");
        });
    }


    public static void openChatDetail(AppCompatActivity activity, String chatId, String friendUsername) {
        Intent intent = new Intent(activity, ChatDetailActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("FRIEND_USERNAME", friendUsername);
        activity.startActivity(intent);
    }
}
