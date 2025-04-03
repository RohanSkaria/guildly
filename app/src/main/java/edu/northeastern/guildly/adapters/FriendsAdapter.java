package edu.northeastern.guildly.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private final List<User> friendUsers;

    public FriendsAdapter(List<User> friendUsers) {
        this.friendUsers = friendUsers;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friendUsers.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friendUsers.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivFriendAvatar;
        TextView tvFriendName;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFriendAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvFriendName   = itemView.findViewById(R.id.tvFriendName);
        }

        void bind(User friend) {
            // Username
            if (!TextUtils.isEmpty(friend.username)) {
                tvFriendName.setText(friend.username);
            } else {
                tvFriendName.setText("Friend");
            }

            // Avatar
            int resourceId;
            switch (friend.profilePicUrl == null ? "" : friend.profilePicUrl) {
                case "gamer": resourceId = R.drawable.gamer; break;
                case "man":   resourceId = R.drawable.man;   break;
                case "girl":  resourceId = R.drawable.girl;  break;
                default:      resourceId = R.drawable.unknown_profile; break;
            }
            ivFriendAvatar.setImageResource(resourceId);
        }
    }
}
