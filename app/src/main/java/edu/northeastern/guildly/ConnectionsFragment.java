package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.northeastern.guildly.adapters.ChatListAdapter;
import edu.northeastern.guildly.adapters.SearchUserAdapter;
import edu.northeastern.guildly.data.Chats;
import edu.northeastern.guildly.data.FriendChatItem;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.data.User;

public class ConnectionsFragment extends Fragment {
    private static final String TAG = "ConnectionsFragment";

    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<FriendChatItem> chatItemsList;
    private EditText editTextFriendUsername;
    private ImageButton buttonAddFriend;
    private TextView friendRequestsBadge;
    private CardView cardFriendRequests;
    private LinearLayout buttonFriendRequests;
    private Map<String, String> usernameLookup = new HashMap<>();

    private DatabaseReference usersRef, chatsRef;
    private String myUserKey;
    private String myEmail;

    // For search dialog
    private AlertDialog searchDialog;
    private RecyclerView searchRecyclerView;
    private SearchUserAdapter searchAdapter;
    private List<User> searchResults = new ArrayList<>();
    private List<String> searchResultKeys = new ArrayList<>();

    private List<FriendChatItem> originalChatItemsList = new ArrayList<>();

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
        chatItemsList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(chatItemsList, item -> {
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

        // Set up the dynamic "Add Friend" dialog
        buttonAddFriend.setOnClickListener(v -> showAddFriendDialog());

        // Set up the chat search functionality
        editTextFriendUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                Log.d("SearchBar", "User typed: " + searchText);
                filterFriendsList(searchText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadAllChats();
            updateFriendRequestsBadge();
        }, 200); // 200ms delay to avoid conflict with search
    }

    /**
     * Show the dynamic search dialog for adding friends
     */
    private void showAddFriendDialog() {
        // Create the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_friend, null);

        // Initialize views
        EditText searchInput = dialogView.findViewById(R.id.search_friend_input);
        searchRecyclerView = dialogView.findViewById(R.id.recyclerViewSearchAddFriend);

        // Set up RecyclerView
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new SearchUserAdapter(searchResults, searchResultKeys, myUserKey,
                new SearchUserAdapter.OnUserActionListener() {
                    @Override
                    public void onAddFriendClicked(String userKey, String username) {
                        sendFriendRequest(userKey);
                    }
                });
        searchRecyclerView.setAdapter(searchAdapter);

        // Create and show dialog
        searchDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add a Friend")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        // Set up text change listener for dynamic filtering
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 1) {
                    searchUsers(query);
                } else {
                    // Clear results if search box is empty
                    searchResults.clear();
                    searchResultKeys.clear();
                    searchAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchDialog.show();
    }

    /**
     * Search for users by username
     */
    private void searchUsers(String query) {
        // Convert query to lowercase for case-insensitive search
        String lowercaseQuery = query.toLowerCase();

        // First, get current user's friends list to filter out friends from search results
        usersRef.child(myUserKey).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> currentFriends = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot friendSnap : snapshot.getChildren()) {
                                String friendKey = friendSnap.getValue(String.class);
                                if (friendKey != null) {
                                    currentFriends.add(friendKey);
                                }
                            }
                        }

                        // Now search for users
                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                searchResults.clear();
                                searchResultKeys.clear();

                                for (DataSnapshot userSnap : snapshot.getChildren()) {
                                    try {
                                        User user = userSnap.getValue(User.class);
                                        String userKey = userSnap.getKey();

                                        if (user != null && user.username != null &&
                                                user.username.toLowerCase().contains(lowercaseQuery)) {
                                            // Skip if this is the current user
                                            if (userKey.equals(myUserKey)) {
                                                continue;
                                            }

                                            // Skip if this user is already a friend
                                            if (currentFriends.contains(userKey)) {
                                                continue;
                                            }

                                            // Add to results
                                            searchResults.add(user);
                                            searchResultKeys.add(userKey);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing user", e);
                                    }
                                }

                                searchAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Search cancelled", error.toException());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting friends list", error.toException());
                    }
                });
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
                    loadAllChats();
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
     * Load all active chats where current user is a participant
     */
    private void loadAllChats() {
        if (myEmail == null) return;

        // Clear the existing list
        chatItemsList.clear();

        // Get all chats where current user is a participant
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> relevantChats = new ArrayList<>();
                Map<DataSnapshot, Long> chatTimestamps = new HashMap<>();


                // First find all chats this user is part of
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    Chats chat = chatSnap.getValue(Chats.class);
                    if (chat != null &&
                            chat.participants != null &&
                            chat.participants.contains(myUserKey) &&
                            chat.messages != null &&
                            !chat.messages.isEmpty()) {

                        relevantChats.add(chatSnap);
                        Long updated = chatSnap.child("lastUpdated").getValue(Long.class);
                        chatTimestamps.put(chatSnap, updated != null ? updated : 0L);
                    }

                }

                relevantChats.sort((a, b) ->
                        Long.compare(chatTimestamps.getOrDefault(b, 0L), chatTimestamps.getOrDefault(a, 0L))
                );

                // If no chats found, load friends as potential chats instead
                if (relevantChats.isEmpty()) {
                    loadFriendsAsPotentialChats();
                    return;
                }

                // Process each relevant chat
                for (DataSnapshot chatSnap : relevantChats) {
                    Chats chat = chatSnap.getValue(Chats.class);
                    if (chat == null || chat.participants == null) continue;

                    String chatId = chat.chatId;
                    String otherUserKey = null;

                    // Find the other participant
                    for (String participantKey : chat.participants) {
                        if (!participantKey.equals(myUserKey)) {
                            otherUserKey = participantKey;
                            break;
                        }
                    }

                    if (otherUserKey == null) continue; // Skip if no other participant

                    // Get username for the other user
                    getFriendUsername(otherUserKey, chatId, chat);
                }

                originalChatItemsList.clear();
                originalChatItemsList.addAll(new ArrayList<>(chatItemsList));
                chatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading chats", error.toException());
            }
        });
    }

    // Helper method to get username and create chat item
    private void getFriendUsername(String friendKey, String chatId, Chats chat) {
        usersRef.child(friendKey).child("username").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String friendUsername = snapshot.getValue(String.class);
                        if (friendUsername == null) {
                            friendUsername = friendKey;
                        }

                        String lastMessageText = "No messages yet";
                        int lastMsgIcon = -1;
                        String timestamp = "";

                        // Find last message if any
                        if (chat.messages != null && !chat.messages.isEmpty()) {
                            Message lastMsg = findLastMessage(chat);
                            if (lastMsg != null) {
                                lastMessageText = lastMsg.content;
                                timestamp = formatTimestamp(lastMsg.timestamp);

                                // Set message icon
                                lastMsgIcon = "READ".equals(lastMsg.status)
                                        ? R.drawable.ic_msg_hollow
                                        : R.drawable.ic_msg_solid;
                            }
                        }

                        FriendChatItem item = new FriendChatItem(
                                friendKey,
                                friendUsername,
                                chatId,
                                lastMessageText,
                                lastMsgIcon,
                                timestamp
                        );
                        chatItemsList.add(item);
                        chatListAdapter.notifyDataSetChanged();

                        // Update original list
                        if (!originalChatItemsList.contains(item)) {
                            originalChatItemsList.add(item);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting username", error.toException());
                    }
                }
        );
    }

    /**
     * Load all usernames to avoid multiple Firebase calls
     */
    private void loadAllUsernames() {
        usernameLookup.clear();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String key = userSnap.getKey();
                    if (key != null) {
                        String username = userSnap.child("username").getValue(String.class);
                        if (username != null) {
                            usernameLookup.put(key, username);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading usernames", error.toException());
            }
        });
    }

    /**
     * Load friends as potential chat options when no active chats exist
     */
    private void loadFriendsAsPotentialChats() {
        usersRef.child(myUserKey).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists() || !snapshot.hasChildren()) {
                            // No friends
                            return;
                        }

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String friendKey = ds.getValue(String.class);
                            if (friendKey != null && !friendKey.trim().isEmpty()) {
                                usersRef.child(friendKey).child("username")
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                                String username = userSnap.getValue(String.class);
                                                String friendUsername = username != null ? username : friendKey;

                                                FriendChatItem item = new FriendChatItem(
                                                        friendKey,
                                                        friendUsername,
                                                        null, // no chat yet
                                                        "Say hello to " + friendUsername,
                                                        -1,
                                                        ""
                                                );
                                                chatItemsList.add(item);
                                                originalChatItemsList.add(item);
                                                chatListAdapter.notifyDataSetChanged();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {}
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
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


    private void filterFriendsList(String searchText) {
        chatItemsList.clear();

        if (TextUtils.isEmpty(searchText)) {
            // Show only active chats with messages (already handled in loadAllChats)
            chatItemsList.addAll(originalChatItemsList);
            chatListAdapter.notifyDataSetChanged();
            return;
        }

        // Keep track of already-added user keys
        Set<String> addedKeys = new HashSet<>();

        // Add filtered items from chats
        for (FriendChatItem item : originalChatItemsList) {
            if (item.friendUsername.toLowerCase().contains(searchText.toLowerCase())) {
                chatItemsList.add(item);
                addedKeys.add(item.friendKey);
            }
        }

        // Search friends list for usernames not already added
        usersRef.child(myUserKey).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String friendKey = ds.getValue(String.class);
                            if (friendKey == null || addedKeys.contains(friendKey)) continue;

                            usersRef.child(friendKey).child("username")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnap) {
                                            String username = userSnap.getValue(String.class);
                                            if (username != null &&
                                                    username.toLowerCase().contains(searchText.toLowerCase())) {

                                                FriendChatItem item = new FriendChatItem(
                                                        friendKey,
                                                        username,
                                                        null,
                                                        "Say hello to " + username,
                                                        -1,
                                                        ""
                                                );
                                                chatItemsList.add(item);
                                                chatListAdapter.notifyDataSetChanged();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "filter: username fetch fail", error.toException());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "filter: friend list fetch fail", error.toException());
                    }
                });
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