package edu.northeastern.guildly;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

import edu.northeastern.guildly.adapters.ChatDetailAdapter;
import edu.northeastern.guildly.data.Chats;
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

        recyclerViewChatDetail = findViewById(R.id.recyclerViewChatDetail);
        editTextMessageInput = findViewById(R.id.editTextMessageInput);
        buttonSend = findViewById(R.id.buttonSend);

        // Setup the RecyclerView
        recyclerViewChatDetail.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();

        // pass my user key so adapter knows how to differentiate "mine" vs "theirs"
        String myEmail = MainActivity.currentUserEmail;
        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER";

        chatDetailAdapter = new ChatDetailAdapter(messageList, myUserKey);
        recyclerViewChatDetail.setAdapter(chatDetailAdapter);

        // 1) Get chatId from Intent
        chatId = getIntent().getStringExtra("CHAT_ID");

        // 2) Reference "chats/<chatId>" in DB
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

        loadMessages();
        setupSendButton();
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

                        if (!msg.senderId.equals(myUserKey) && "SENT".equals(msg.status)) {
                            msgSnap.getRef().child("status").setValue("READ");
                            msg.status = "READ";
                        }
                        messageList.add(msg);
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
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editTextMessageInput.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                // Build message
                Message msg = new Message();
                msg.senderId = myUserKey;
                msg.content = text;
                msg.timestamp = System.currentTimeMillis();
                msg.status = "SENT";

                // Push to DB
                DatabaseReference pushRef = chatRef.child("messages").push();
                pushRef.setValue(msg);

                editTextMessageInput.setText("");
            }
        });
    }
}
