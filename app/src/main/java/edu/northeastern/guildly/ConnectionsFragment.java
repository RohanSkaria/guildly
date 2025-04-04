package edu.northeastern.guildly;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.adapters.ConnectionsAdapter;
import edu.northeastern.guildly.data.Friend;
import edu.northeastern.guildly.data.Habit;
import edu.northeastern.guildly.data.User;

public class ConnectionsFragment extends Fragment {

    // EditText for friend username input
    private EditText friendInput;
    private RecyclerView connectionsRecyclerView;
    private TextView friendRequestsBadge;

    // Buttons from layout
    private View buttonAddFriend;
    private View buttonFriendRequests;

    // We'll remove the hardcoded email and use MainActivity.currentUserEmail
    private String myEmail;
    private String myUserKey;  // sanitized email key

    private DatabaseReference usersRef;
    private DatabaseReference userRef;

    private List<String> myFriendsList; // store friend keys
    private ConnectionsAdapter connectionsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_connections, container, false);

        // 1) Envelope Icon
        ImageView envelopeIcon = root.findViewById(R.id.buttonOpenChatList);
        envelopeIcon.setOnClickListener(v -> {
            // Start ChatListActivity
            Intent intent = new Intent(getContext(), ChatListActivity.class);
            startActivity(intent);
        });

        // The rest of your existing code:
        friendInput = root.findViewById(R.id.editTextFriendUsername);
        connectionsRecyclerView = root.findViewById(R.id.connections_list);
        buttonAddFriend = root.findViewById(R.id.buttonAddFriend);
        buttonFriendRequests = root.findViewById(R.id.buttonFriendRequests);
        friendRequestsBadge = root.findViewById(R.id.friendRequestsBadge);

        myEmail = MainActivity.currentUserEmail;
        if (myEmail == null) {
            Toast.makeText(requireContext(),
                    "No logged-in user. Please log in first.",
                    Toast.LENGTH_LONG).show();
        }

        myUserKey = (myEmail != null) ? myEmail.replace(".", ",") : "NO_USER_KEY";

        connectionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myFriendsList = new ArrayList<>();
        connectionsAdapter = new ConnectionsAdapter(myFriendsList);
        connectionsRecyclerView.setAdapter(connectionsAdapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        userRef = usersRef.child(myUserKey);

        if (myEmail != null) {
            loadMyFriends();
        }

        setupInputListener();
        setupFriendRequestsButton();
        updateFriendRequestsBadge();


        RecyclerView leaderboardRecyclerView = root.findViewById(R.id.leaderboard);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        TextView globalHeader = root.findViewById(R.id.global_header);
        globalHeader.setText("Global Leaderboard");


        loadLeaderboardData(leaderboardRecyclerView);

        return root;
    }


    private void loadLeaderboardData(RecyclerView leaderboardRecyclerView) {
        // Example implementation - you'll need to populate with real data based on friend streaks
        List<Friend> friendsList = new ArrayList<>();

        // If you have friends, look up their habit data and add to leaderboard
        if (myFriendsList != null && !myFriendsList.isEmpty()) {
            for (String friendKey : myFriendsList) {
                usersRef.child(friendKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User friendUser = snapshot.getValue(User.class);
                        if (friendUser != null) {
                            // Get the friend's top habit streak
                            int topStreak = 0;

                            // Look in their habits for the top streak
                            if (snapshot.child("habits").exists()) {
                                for (DataSnapshot habitSnap : snapshot.child("habits").getChildren()) {
                                    Habit habit = habitSnap.getValue(Habit.class);
                                    if (habit != null && habit.isTracked() && habit.getStreakCount() > topStreak) {
                                        topStreak = habit.getStreakCount();
                                    }
                                }
                            }

                            // Get profile image resource
                            int profileImageResource;
                            switch (friendUser.profilePicUrl == null ? "" : friendUser.profilePicUrl) {
                                case "gamer": profileImageResource = R.drawable.gamer; break;
                                case "man": profileImageResource = R.drawable.man; break;
                                case "girl": profileImageResource = R.drawable.girl; break;
                                default: profileImageResource = R.drawable.unknown_profile; break;
                            }

                            // Add to the leaderboard list
                            friendsList.add(new Friend(
                                    friendUser.username != null ? friendUser.username : "Friend",
                                    topStreak,
                                    profileImageResource
                            ));

                            // Also add the current user to the leaderboard
                            if (friendsList.size() == myFriendsList.size()) {
                                addCurrentUserToLeaderboard(friendsList, leaderboardRecyclerView);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
            }
        } else {
            // No friends, just show the current user
            addCurrentUserToLeaderboard(friendsList, leaderboardRecyclerView);
        }
    }

    private void addCurrentUserToLeaderboard(List<Friend> friendsList, RecyclerView leaderboardRecyclerView) {
        // Add the current user to the leaderboard
        usersRef.child(myUserKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    // Get current user's top streak
                    int topStreak = 0;

                    if (snapshot.child("habits").exists()) {
                        for (DataSnapshot habitSnap : snapshot.child("habits").getChildren()) {
                            Habit habit = habitSnap.getValue(Habit.class);
                            if (habit != null && habit.isTracked() && habit.getStreakCount() > topStreak) {
                                topStreak = habit.getStreakCount();
                            }
                        }
                    }

                    // Get profile image resource
                    int profileImageResource;
                    switch (currentUser.profilePicUrl == null ? "" : currentUser.profilePicUrl) {
                        case "gamer": profileImageResource = R.drawable.gamer; break;
                        case "man": profileImageResource = R.drawable.man; break;
                        case "girl": profileImageResource = R.drawable.girl; break;
                        default: profileImageResource = R.drawable.unknown_profile; break;
                    }

                    // Add current user to list
                    friendsList.add(new Friend(
                            currentUser.username != null ? currentUser.username : "Me",
                            topStreak,
                            profileImageResource
                    ));

                    // Sort the list by streak count in descending order
                    Collections.sort(friendsList, (f1, f2) -> Integer.compare(f2.getStreakCount(), f1.getStreakCount()));

                    // Create and set the adapter
                    LeaderboardAdapter adapter = new LeaderboardAdapter(friendsList);
                    leaderboardRecyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    /**
     * Loads the current user's 'friends' list from DB and updates the RecyclerView.
     */
    private void loadMyFriends() {
        usersRef.child(myUserKey).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myFriendsList.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot friendSnap : snapshot.getChildren()) {
                                String friendKey = friendSnap.getValue(String.class);
                                if (friendKey != null) {
                                    myFriendsList.add(friendKey);
                                }
                            }
                        }
                        connectionsAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ConnectionsFragment",
                                "Failed to load friends", error.toException());
                    }
                });
    }

    /**
     * Set up the "Add Friend" button to read text from the friendInput EditText.
     */
    private void setupInputListener() {
        buttonAddFriend.setOnClickListener(v -> {
            if (myEmail == null) {
                Toast.makeText(getContext(),
                        "No logged-in user. Can't add friends.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String query = friendInput.getText().toString().trim();
            if (TextUtils.isEmpty(query)) {
                Toast.makeText(getContext(), "Type a username", Toast.LENGTH_SHORT).show();
                return;
            }
            findUserByUsername(query);
        });
    }

    /**
     * Query the "users" node for a user whose username equals the search input.
     * If found, send a friend request to that user.
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
                Log.e("ConnectionsFragment",
                        "findUserByUsername canceled", error.toException());
            }
        });
    }

    /**
     * Send a friend request by setting 'friendRequests[myUserKey] = "pending"' on the target user.
     */
    private void sendFriendRequest(String targetUserKey) {
        DatabaseReference targetUserRef = usersRef.child(targetUserKey);
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
                String status = targetUser.friendRequests.get(myUserKey);
                if ("pending".equals(status)) {
                    // already requested
                    return Transaction.success(currentData);
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
                    Log.e("ConnectionsFragment",
                            "sendFriendRequest error", error.toException());
                    Toast.makeText(getContext(),
                            "Error sending request: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else if (!committed) {
                    Toast.makeText(getContext(),
                            "Request not committed",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "Friend request sent!",
                            Toast.LENGTH_SHORT).show();
                    // Update badge
                    updateFriendRequestsBadge();
                }
            }
        });
    }

    /**
     * Set up the "Friend Requests" button to show a popup listing pending requests.
     */
    private void setupFriendRequestsButton() {
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
                            Log.e("ConnectionsFragment",
                                    "friendRequests cancelled", error.toException());
                        }
                    }
            );
        });
    }

    /**
     * Update the badge on the Friend Requests button to show
     * the number of pending requests. Hide if none.
     */
    private void updateFriendRequestsBadge() {
        if (myEmail == null) {
            friendRequestsBadge.setVisibility(View.GONE);
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
                            friendRequestsBadge.setVisibility(View.VISIBLE);
                        } else {
                            friendRequestsBadge.setVisibility(View.GONE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // optional: handle error
                    }
                });
    }

    /**
     * Show a chain of AlertDialogs for each pending friend request,
     * allowing the user to accept or reject each one.
     */
    private void showFriendRequestsDialog(List<String> pendingKeys) {
        processNextRequest(pendingKeys, 0);
    }

    private void processNextRequest(List<String> pendingKeys, int index) {
        if (index >= pendingKeys.size()) {
            return;
        }
        String requesterKey = pendingKeys.get(index);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Friend Request");
        builder.setMessage("User: " + requesterKey + " wants to be friends.");
        builder.setPositiveButton("ACCEPT", (dialog, which) -> {
            acceptFriendRequest(requesterKey);
            processNextRequest(pendingKeys, index + 1);
        });
        builder.setNegativeButton("REJECT", (dialog, which) -> {
            rejectFriendRequest(requesterKey);
            processNextRequest(pendingKeys, index + 1);
        });
        builder.setOnCancelListener(dialog -> processNextRequest(pendingKeys, index + 1));
        builder.show();
    }

    /**
     * Accept a friend request: remove it from my friendRequests,
     * and add each other to 'friends'.
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
                    loadMyFriends(); // refresh
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
                    Log.e("ConnectionsFragment",
                            "addMeToRequesterFriends error", error.toException());
                }
            }
        });
    }

    /**
     * Reject a friend request by removing it from my friendRequests.
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
                }
            }
        });
    }
}
