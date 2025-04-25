package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

public class ConnectionsAdapter extends RecyclerView.Adapter<ConnectionsAdapter.ConnectionsViewHolder> {

    private List<String> friendKeys;
    private DatabaseReference usersRef;
    private Map<String, String> usernameCache = new HashMap<>();

    public ConnectionsAdapter(List<String> friendKeys) {
        this.friendKeys = friendKeys;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public ConnectionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection, parent, false);
        return new ConnectionsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectionsViewHolder holder, int position) {
        String friendKey = friendKeys.get(position);

        if (usernameCache.containsKey(friendKey)) {
            String cachedUsername = usernameCache.get(friendKey);
            holder.usernameText.setText(cachedUsername);
        } else {
            holder.usernameText.setText("Loading...");
        }


        holder.profileImage.setImageResource(R.drawable.unknown_profile);

        usersRef.child(friendKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User friendUser = snapshot.getValue(User.class);
                        if (friendUser != null) {

                            if (friendUser.username != null) {
                                usernameCache.put(friendKey, friendUser.username);
                                holder.usernameText.setText(friendUser.username);
                            } else {
                                holder.usernameText.setText(friendKey + " (not found)");
                            }


                            int resourceId;
                            if ("gamer".equals(friendUser.profilePicUrl)) {
                                resourceId = R.drawable.gamer;
                            } else if ("man".equals(friendUser.profilePicUrl)) {
                                resourceId = R.drawable.man;
                            } else if ("girl".equals(friendUser.profilePicUrl)) {
                                resourceId = R.drawable.girl;
                            } else {
                                resourceId = R.drawable.unknown_profile;
                            }
                            holder.profileImage.setImageResource(resourceId);
                        } else {
                            holder.usernameText.setText(friendKey + " (not found)");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.usernameText.setText(friendKey + " (error)");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return friendKeys.size();
    }

    public static class ConnectionsViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        ImageView profileImage;

        public ConnectionsViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.text_username);
            profileImage = itemView.findViewById(R.id.image_profile);
        }
    }
}