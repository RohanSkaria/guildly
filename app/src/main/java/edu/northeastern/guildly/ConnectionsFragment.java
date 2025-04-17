package edu.northeastern.guildly;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
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
import edu.northeastern.guildly.data.User;

public class ConnectionsFragment extends Fragment {
    private static final String TAG = "ConnectionsFragment";

    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<FriendChatItem> friendChatList;
    private EditText editTextFriendUsername;
    private ImageButton buttonAddFriend;
    private TextView friendRequestsBadge;
    private CardView cardFriendRequests;
    private LinearLayout buttonFriendRequests;

    private DatabaseReference usersRef, chatsRef;
    private String myUserKey;
    private String myEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_connections, container, false);

        // Initialize views
        recyclerViewChatList = root.findViewById(R.id.recyclerViewChatList);
        editTextFriendUsername = root.findViewById(R.id.editTextFriendUsername);
        buttonAddFriend = root.findViewById(R.id.buttonAddFriend);
        friendRequestsBadge = root.findViewById(R.id.friendRequestsBadge);
        cardFriendRequests = root.findViewById(R.id.cardFriendRequests);
        buttonFriendRequests = root.findViewById(R.id.buttonFriendRequests);

        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get current user info
        myEmail = MainActivity.currentUserEmail;
        if (myEmail == null) {
            Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show();
            return root;
        }
        myUserKey = myEmail.replace(".", ",");

        // Initialize chat list
        friendChatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(friendChatList, item -> {
            if (item.chatId != null) {
                // If an existing chatId, go to detail
                ChatDetailActivity.openChatDetail(
                        (AppCompatActivity) requireActivity(),
                        item.chatId,
                        item.friendUsername
                );
            } else {
                // Otherwise create a new chat
                createNewChat(item.friendKey, item.friendUsername);
            }
        });
        recyclerViewChatList.setAdapter(chatListAdapter);

        // Firebase references
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        buttonAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the popup layout
                LayoutInflater inflater = LayoutInflater.from(requireContext());
                View dialogView = inflater.inflate(R.layout.dialog_add_friend, null); // Make sure this layout exists

                // Find EditText in the popup layout
                EditText searchInput = dialogView.findViewById(R.id.search_friend_input);

                // Build and show the dialog
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("Add a Friend")
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .create();

                dialog.show();
            }
        });

//        // Set up add friend button old logic
//        buttonAddFriend.setOnClickListener(v -> {
//            String username = editTextFriendUsername.getText().toString().trim();
//            if (TextUtils.isEmpty(username)) {
//                Toast.makeText(getContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            findUserByUsername(username);
//        });

        // Set up friend requests button
        buttonFriendRequests.setOnClickListener(v -> {
            if (myEmail == null) {
                Toast.makeText(getContext(),
                        "No logged-in user. Cannot view friend requests.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            usersRef.child(myUserKey).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User me = snapshot.getValue(User.class);
                            if (me == null) {
                                Toast.makeText(getContext(),
                                        "Error: Your user not found.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            List<String> pendingKeys = new ArrayList<>();
                            if (me.friendRequests != null) {
                                for (Map.Entry<String, String> entry :
                                        me.friendRequests.entrySet()) {
                                    if ("pending".equals(entry.getValue())) {
                                        pendingKeys.add(entry.getKey());
                                    }
                                }
                            }
                            if (pendingKeys.isEmpty()) {
                                Toast.makeText(getContext(),
                                        "No pending friend requests.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                showFriendRequestsDialog(pendingKeys);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "friendRequests cancelled", error.toException());
                        }
                    }
            );
        });

        updateFriendRequestsBadge();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllMyFriends();
        updateFriendRequestsBadge();
        if (chatListAdapter != null) {
            chatListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Find user by username and send friend request
     */
    private void findUserByUsername(String searchUsername) {
        if (myEmail == null) return;

        Query query = usersRef.orderByChild("username").equalTo(searchUsername);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(),
                            "No user found with username: " + searchUsername,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Usually only one match if usernames are unique
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String targetUserKey = userSnap.getKey(); // e.g. "bob@example,com"

                    if (myUserKey.equals(targetUserKey)) {
                        Toast.makeText(getContext(),
                                "That's you! Can't friend yourself.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendFriendRequest(targetUserKey);
                    return; // stop after first match
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "findUserByUsername canceled", error.toException());
            }
        });
    }

    /**
     * Send a friend request
     */
    private void sendFriendRequest(String targetUserKey) {
        DatabaseReference targetUserRef = usersRef.child(targetUserKey);
        final int[] resultCode = {0}; // 0: success, 1: already friends, 2: already requested

        targetUserRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User targetUser = currentData.getValue(User.class);
                if (targetUser == null) {
                    return Transaction.success(currentData);
                }

                if (targetUser.friendRequests == null) {
                    targetUser.friendRequests = new HashMap<>();
                }

                if (targetUser.friends != null && targetUser.friends.contains(myUserKey)) {
                    resultCode[0] = 1;
                    return Transaction.abort();
                }

                String status = targetUser.friendRequests.get(myUserKey);
                if ("pending".equals(status)) {
                    resultCode[0] = 2;
                    return Transaction.abort();
                }

                targetUser.friendRequests.put(myUserKey, "pending");
                currentData.setValue(targetUser);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(getContext(), "Error sending request", Toast.LENGTH_LONG).show();
                } else if (!committed) {
                    if (resultCode[0] == 1) {
                        Toast.makeText(getContext(), "You're already friends", Toast.LENGTH_SHORT).show();
                    } else if (resultCode[0] == 2) {
                        Toast.makeText(getContext(), "Friend request already sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Request not committed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Friend request sent!", Toast.LENGTH_SHORT).show();
                    editTextFriendUsername.setText("");
                    updateFriendRequestsBadge();
                }
            }
        });
    }

    /**
     * Show friend request dialog
     */
    private void showFriendRequestsDialog(List<String> pendingKeys) {
        if (pendingKeys.isEmpty() || getContext() == null) {
            return;
        }

        // Create a list of usernames to display
        List<String> displayNames = new ArrayList<>();
        final int[] processed = {0};

        for (String key : pendingKeys) {
            usersRef.child(key).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String username = snapshot.getValue(String.class);
                    displayNames.add(username != null ? username : key);
                    processed[0]++;

                    // When all names are loaded, show the dialog
                    if (processed[0] == pendingKeys.size()) {
                        showRequestsListDialog(pendingKeys, displayNames);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    displayNames.add(key);
                    processed[0]++;

                    if (processed[0] == pendingKeys.size()) {
                        showRequestsListDialog(pendingKeys, displayNames);
                    }
                }
            });
        }
    }

    private void showRequestsListDialog(List<String> pendingKeys, List<String> displayNames) {
        if (getContext() == null) return;

        // Create dialog with all friend requests in a list
        String[] items = new String[displayNames.size()];
        for (int i = 0; i < displayNames.size(); i++) {
            items[i] = displayNames.get(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Friend Requests");
        builder.setItems(items, (dialog, which) -> {
            String requesterKey = pendingKeys.get(which);
            showAcceptRejectDialog(requesterKey, displayNames.get(which));
        });
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showAcceptRejectDialog(String requesterKey, String displayName) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Friend Request");
        builder.setMessage(displayName + " wants to be friends.");
        builder.setPositiveButton("ACCEPT", (dialog, which) -> acceptFriendRequest(requesterKey));
        builder.setNegativeButton("REJECT", (dialog, which) -> rejectFriendRequest(requesterKey));
        builder.show();
    }

    /**
     * Accept friend request
     */
    private void acceptFriendRequest(String requesterKey) {
        if (myEmail == null) return;
        DatabaseReference meRef = usersRef.child(myUserKey);
        meRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User me = currentData.getValue(User.class);
                if (me == null) {
                    return Transaction.success(currentData);
                }
                if (me.friendRequests != null) {
                    me.friendRequests.remove(requesterKey);
                }
                if (me.friends == null) {
                    me.friends = new ArrayList<>();
                }
                if (!me.friends.contains(requesterKey)) {
                    me.friends.add(requesterKey);
                }
                currentData.setValue(me);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(getContext(),
                            "Error accepting request: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else if (committed) {
                    addMeToRequesterFriends(requesterKey);
                    loadAllMyFriends();
                    updateFriendRequestsBadge();
                }
            }
        });
    }

    private void addMeToRequesterFriends(String requesterKey) {
        if (myEmail == null) return;
        DatabaseReference requesterRef = usersRef.child(requesterKey);
        requesterRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User requester = currentData.getValue(User.class);
                if (requester == null) {
                    return Transaction.success(currentData);
                }
                if (requester.friends == null) {
                    requester.friends = new ArrayList<>();
                }
                if (!requester.friends.contains(myUserKey)) {
                    requester.friends.add(myUserKey);
                }
                currentData.setValue(requester);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "addMeToRequesterFriends error", error.toException());
                }
            }
        });
    }

    /**
     * Reject friend request
     */
    private void rejectFriendRequest(String requesterKey) {
        if (myEmail == null) return;
        DatabaseReference meRef = usersRef.child(myUserKey);
        meRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User me = currentData.getValue(User.class);
                if (me == null) {
                    return Transaction.success(currentData);
                }
                if (me.friendRequests != null) {
                    me.friendRequests.remove(requesterKey);
                }
                currentData.setValue(me);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error,
                                   boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(getContext(),
                            "Error rejecting request: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else if (committed) {
                    Toast.makeText(getContext(),
                            "Request rejected.",
                            Toast.LENGTH_SHORT).show();
                    updateFriendRequestsBadge();
                }
            }
        });
    }

    /**
     * Update friend request badge
     */
    private void updateFriendRequestsBadge() {
        if (myEmail == null) {
            cardFriendRequests.setVisibility(View.GONE);
            return;
        }
        usersRef.child(myUserKey).child("friendRequests")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String status = ds.getValue(String.class);
                            if ("pending".equals(status)) {
                                count++;
                            }
                        }
                        if (count > 0) {
                            friendRequestsBadge.setText(String.valueOf(count));
                            cardFriendRequests.setVisibility(View.VISIBLE);
                        } else {
                            cardFriendRequests.setVisibility(View.GONE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error updating friend request badge", error.toException());
                    }
                });
    }

    /**
     * Load all friends
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
     * Fetch friend data
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
     * Find existing chat
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
     * Find last message
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
     * Format timestamp
     */
    private String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    /**
     * Create new chat
     */
    private void createNewChat(String friendKey, String friendUsername) {
        if (friendKey == null) {
            Toast.makeText(getContext(),
                    "Cannot start chat: friend key is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a new chat ID
        String newChatId = chatsRef.push().getKey();
        if (newChatId == null) {
            Toast.makeText(getContext(), "Error creating chat", Toast.LENGTH_SHORT).show();
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
                            (AppCompatActivity) requireActivity(),
                            newChatId,
                            friendUsername
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createNewChat failed", e);
                    Toast.makeText(getContext(),
                            "Failed to create chat: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}