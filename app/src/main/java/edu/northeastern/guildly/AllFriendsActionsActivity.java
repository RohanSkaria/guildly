package edu.northeastern.guildly;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.guildly.adapters.AllFriendsActionsAdapter;
import edu.northeastern.guildly.data.Chats;
import edu.northeastern.guildly.data.User;

public class AllFriendsActionsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFriends;
    private AllFriendsActionsAdapter adapter;

    private String myUserKey;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_friends_actions);

        // Setup toolbar with back arrow
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Friends");
        }

        recyclerViewFriends = findViewById(R.id.recyclerViewAllFriends);
        recyclerViewFriends.setLayoutManager(new LinearLayoutManager(this));

        // Determine current user
        String myEmail = MainActivity.currentUserEmail;
        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER";

        userRef = FirebaseDatabase.getInstance().getReference("users").child(myUserKey);

        adapter = new AllFriendsActionsAdapter(
                new ArrayList<>(),
                // onProfileClicked
                friendKey -> {
                    // Open the new FriendProfileActivity
                    FriendProfileActivity.openProfile(AllFriendsActionsActivity.this, friendKey);
                },
                // onMessageClicked
                friendKey -> {
                    findOrCreateChatThenOpen(friendKey);
                },
                // onDeleteClicked
                friendKey -> {
                    confirmDeleteFriend(friendKey);
                }
        );
        recyclerViewFriends.setAdapter(adapter);

        loadAllFriends();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Back arrow in the toolbar
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAllFriends() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User me = snapshot.getValue(User.class);
                if (me == null || me.friends == null || me.friends.isEmpty()) {
                    Toast.makeText(AllFriendsActionsActivity.this,
                            "You have no friends!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update adapter with friendKeys
                adapter.setFriendKeys(me.friends);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AllFriendsActionsActivity.this,
                        "Failed to load friends", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteFriend(String friendKey) {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Friend")
                .setMessage("Are you sure you want to remove this friend?")
                .setPositiveButton("Yes", (dialogInterface, i) -> deleteFriend(friendKey))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteFriend(String friendKey) {
        // Remove friendKey from myUserKey's friend list
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User me = snapshot.getValue(User.class);
                if (me == null || me.friends == null) return;

                if (me.friends.contains(friendKey)) {
                    me.friends.remove(friendKey);
                    userRef.setValue(me)  // overwrites, so ensure "habits" field is in User class
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(AllFriendsActionsActivity.this,
                                            "Friend removed", Toast.LENGTH_SHORT).show());
                }
                // Remove myUserKey from the friend's friend list
                DatabaseReference friendRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(friendKey);
                friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot friendSnap) {
                        User friend = friendSnap.getValue(User.class);
                        if (friend == null || friend.friends == null) return;

                        if (friend.friends.contains(myUserKey)) {
                            friend.friends.remove(myUserKey);
                            friendRef.setValue(friend)  // same caution
                                    .addOnSuccessListener(aVoid -> {
                                        // reload to refresh the UI
                                        loadAllFriends();
                                    });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * If there's already a chat with me & friendKey, open it.
     * Otherwise create new chat in /chats.
     */
    private void findOrCreateChatThenOpen(String friendKey) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        // Do a single read of all chats to find one that has these two participants
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingChatId = null;

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    // read the chat object
                    Chats chatObj = chatSnap.getValue(Chats.class);
                    if (chatObj == null || chatObj.participants == null) continue;

                    if (chatObj.participants.size() == 2
                            && chatObj.participants.contains(myUserKey)
                            && chatObj.participants.contains(friendKey)) {
                        existingChatId = chatObj.chatId;
                        break;
                    }
                }

                if (existingChatId != null) {
                    // Use your existing openChatDetail
                    openChatDetail(existingChatId, friendKey);
                } else {
                    // Create new chat
                    String newChatId = chatsRef.push().getKey();
                    if (newChatId == null) {
                        Toast.makeText(AllFriendsActionsActivity.this,
                                "Error creating chat", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Chats newChat = new Chats();
                    newChat.chatId = newChatId;
                    List<String> parts = new ArrayList<>();
                    parts.add(myUserKey);
                    parts.add(friendKey);
                    newChat.participants = parts;

                    chatsRef.child(newChatId).setValue(newChat)
                            .addOnSuccessListener(aVoid -> {
                                openChatDetail(newChatId, friendKey);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AllFriendsActionsActivity.this,
                                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AllFriendsActionsAct", "findOrCreateChat cancelled", error.toException());
            }
        });
    }

    private void openChatDetail(String chatId, String friendKey) {
        // Optionally fetch the friend's username for the title
        DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference("users")
                .child(friendKey)
                .child("username");

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String friendUsername = snapshot.getValue(String.class);
                if (friendUsername == null) friendUsername = "Friend";

                ChatDetailActivity.openChatDetail(
                        AllFriendsActionsActivity.this,
                        chatId,
                        friendUsername
                );
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
