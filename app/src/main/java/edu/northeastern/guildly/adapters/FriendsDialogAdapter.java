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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

public class FriendsDialogAdapter extends RecyclerView.Adapter<FriendsDialogAdapter.ViewHolder> {

    public interface OnFriendActionListener {
        void onProfileClick(String friendKey);
        void onMessageClick(String friendKey);
        void onDeleteClick(String friendKey);
    }

    private List<String> friendKeys;
    private OnFriendActionListener listener;

    public FriendsDialogAdapter(List<String> friendKeys, OnFriendActionListener listener) {
        this.friendKeys = friendKeys;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_dialog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String friendKey = friendKeys.get(position);

        // Load friend data from Firebase
        DatabaseReference friendRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(friendKey);

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friend = snapshot.getValue(User.class);
                if (friend != null) {
                    // Set friend name
                    holder.textFriendName.setText(friend.username != null ?
                            friend.username : "Friend");

                    // Set friend avatar
                    int resourceId;
                    if ("gamer".equals(friend.profilePicUrl)) {
                        resourceId = R.drawable.gamer;
                    } else if ("man".equals(friend.profilePicUrl)) {
                        resourceId = R.drawable.man;
                    } else if ("girl".equals(friend.profilePicUrl)) {
                        resourceId = R.drawable.girl;
                    } else {
                        resourceId = R.drawable.unknown_profile;
                    }
                    holder.ivFriendAvatar.setImageResource(resourceId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Use default values if error occurs
                holder.textFriendName.setText("Friend");
                holder.ivFriendAvatar.setImageResource(R.drawable.unknown_profile);
            }
        });

        // Set click listeners for action buttons
        holder.iconProfile.setOnClickListener(v -> {
            if (listener != null) listener.onProfileClick(friendKey);
        });

        holder.iconMessage.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(friendKey);
        });

        holder.iconTrash.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(friendKey);
        });
    }

    @Override
    public int getItemCount() {
        return friendKeys.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivFriendAvatar;
        TextView textFriendName;
        ImageView iconProfile, iconMessage, iconTrash;

        ViewHolder(View itemView) {
            super(itemView);
            ivFriendAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            textFriendName = itemView.findViewById(R.id.textFriendName);
            iconProfile = itemView.findViewById(R.id.iconProfile);
            iconMessage = itemView.findViewById(R.id.iconMessage);
            iconTrash = itemView.findViewById(R.id.iconTrash);
        }
    }
}