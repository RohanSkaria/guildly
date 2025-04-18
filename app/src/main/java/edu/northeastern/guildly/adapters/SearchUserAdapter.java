package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.User;

import java.util.List;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {

    public interface OnUserActionListener {
        void onAddFriendClicked(String userKey, String username);
    }

    private List<User> users;
    private List<String> userKeys;
    private String currentUserKey;
    private OnUserActionListener listener;

    public SearchUserAdapter(List<User> users, List<String> userKeys, String currentUserKey, OnUserActionListener listener) {
        this.users = users;
        this.userKeys = userKeys;
        this.currentUserKey = currentUserKey;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        String userKey = userKeys.get(position);

        holder.usernameText.setText(user.username != null ? user.username : "User");

        // Set avatar based on profilePicUrl
        int resourceId;
        switch (user.profilePicUrl == null ? "" : user.profilePicUrl) {
            case "gamer": resourceId = R.drawable.gamer; break;
            case "man":   resourceId = R.drawable.man;   break;
            case "girl":  resourceId = R.drawable.girl;  break;
            default:      resourceId = R.drawable.unknown_profile; break;
        }
        holder.profileImage.setImageResource(resourceId);

        // Check if this is the current user
        if (userKey.equals(currentUserKey)) {
            holder.addButton.setVisibility(View.GONE);
            holder.statusText.setVisibility(View.VISIBLE);
            holder.statusText.setText("You");
        } else {
            holder.addButton.setVisibility(View.VISIBLE);
            holder.statusText.setVisibility(View.GONE);

            holder.addButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddFriendClicked(userKey, user.username);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateData(List<User> newUsers, List<String> newUserKeys) {
        this.users = newUsers;
        this.userKeys = newUserKeys;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView usernameText;
        Button addButton;
        TextView statusText;

        ViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.image_profile);
            usernameText = itemView.findViewById(R.id.text_username);
            addButton = itemView.findViewById(R.id.button_add_friend);
            statusText = itemView.findViewById(R.id.text_status);
        }
    }
}