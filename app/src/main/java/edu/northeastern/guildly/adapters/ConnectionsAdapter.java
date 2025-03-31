package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.data.User;

public class ConnectionsAdapter extends RecyclerView.Adapter<ConnectionsAdapter.ConnectionsViewHolder> {

    private List<String> friendKeys; // e.g., ["john@example,com", "jane@example,com"]
    private DatabaseReference usersRef;

    // Optional in-memory cache to avoid refetching the same user
    private Map<String, String> usernameCache = new HashMap<>();

    public ConnectionsAdapter(List<String> friendKeys) {
        this.friendKeys = friendKeys;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public ConnectionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ConnectionsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectionsViewHolder holder, int position) {
        String friendKey = friendKeys.get(position);

        // If we have a cached username for this friendKey, use it immediately
        if (usernameCache.containsKey(friendKey)) {
            String cachedUsername = usernameCache.get(friendKey);
            holder.textView.setText(cachedUsername);
            return;
        }

        // Otherwise, temporarily show friendKey while we fetch from DB
        holder.textView.setText("Loading...");

        // Fetch the user’s username from the database
        usersRef.child(friendKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User friendUser = snapshot.getValue(User.class);
                        if (friendUser != null && friendUser.username != null) {
                            // Cache the username to avoid multiple fetches
                            usernameCache.put(friendKey, friendUser.username);
                            holder.textView.setText(friendUser.username);
                        } else {
                            // If not found or no username field, fallback
                            holder.textView.setText(friendKey + " (not found)");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.textView.setText(friendKey + " (error)");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return friendKeys.size();
    }

    public static class ConnectionsViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ConnectionsViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
