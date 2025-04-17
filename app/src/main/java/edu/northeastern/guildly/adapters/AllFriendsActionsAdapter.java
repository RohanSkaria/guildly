package edu.northeastern.guildly.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

public class AllFriendsActionsAdapter
        extends RecyclerView.Adapter<AllFriendsActionsAdapter.ViewHolder> {

    /** Click listeners for each action */
    public interface OnProfileClicked {
        void openProfile(String friendKey);
    }
    public interface OnMessageClicked {
        void openChat(String friendKey);
    }
    public interface OnDeleteClicked {
        void deleteFriend(String friendKey);
    }

    private List<String> friendKeys;     // store friend userKeys
    private OnProfileClicked profileListener;
    private OnMessageClicked messageListener;
    private OnDeleteClicked deleteListener;

    public AllFriendsActionsAdapter(List<String> friendKeys,
                                    OnProfileClicked profileListener,
                                    OnMessageClicked messageListener,
                                    OnDeleteClicked deleteListener) {
        this.friendKeys = friendKeys;
        this.profileListener = profileListener;
        this.messageListener = messageListener;
        this.deleteListener = deleteListener;
    }

    public void setFriendKeys(List<String> newKeys) {
        this.friendKeys = newKeys;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AllFriendsActionsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_actions, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AllFriendsActionsAdapter.ViewHolder holder, int position) {
        String friendKey = friendKeys.get(position);
        if (TextUtils.isEmpty(friendKey)) {
            // Just skip
            holder.textFriendName.setText("Unknown");
            return;
        }

        // Load the friend's username
        DatabaseReference friendRef = FirebaseDatabase.getInstance()
                .getReference("users").child(friendKey).child("username");

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String uname = snapshot.getValue(String.class);
                if (!TextUtils.isEmpty(uname)) {
                    holder.textFriendName.setText(uname);
                } else {
                    holder.textFriendName.setText("Friend");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.textFriendName.setText("Friend");
            }
        });

        // Profile icon
        holder.iconProfile.setOnClickListener(v -> {
            if (profileListener != null) {
                profileListener.openProfile(friendKey);
            }
        });

        // Message icon
        holder.iconMessage.setOnClickListener(v -> {
            if (messageListener != null) {
                messageListener.openChat(friendKey);
            }
        });

        // Trash icon
        holder.iconTrash.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.deleteFriend(friendKey);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendKeys.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textFriendName;
        ImageView iconProfile, iconMessage, iconTrash;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFriendName = itemView.findViewById(R.id.textFriendName);
            iconProfile    = itemView.findViewById(R.id.iconProfile);
            iconMessage    = itemView.findViewById(R.id.iconMessage);
            iconTrash      = itemView.findViewById(R.id.iconTrash);
        }
    }
}
