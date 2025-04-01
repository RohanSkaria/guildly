package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.FriendChatItem;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnFriendChatClick {
        void onChatClicked(FriendChatItem item);
    }

    private List<FriendChatItem> friendChatList;
    private OnFriendChatClick listener;

    public ChatListAdapter(List<FriendChatItem> friendChatList,
                           OnFriendChatClick listener) {
        this.friendChatList = friendChatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendChatItem item = friendChatList.get(position);

        holder.textFriendUsername.setText(item.friendUsername);
        holder.textLastMessage.setText(item.lastMessage);
        holder.textTimestamp.setText(item.timestamp);
        holder.imageFriendAvatar.setImageResource(R.drawable.unknown_profile);

        if (item.lastMessageIconRes > 0) {
            holder.imageLastMessageStatus.setVisibility(View.VISIBLE);
            holder.imageLastMessageStatus.setImageResource(item.lastMessageIconRes);
        } else {
            holder.imageLastMessageStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendChatList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textFriendUsername, textLastMessage, textTimestamp;
        ImageView imageLastMessageStatus, imageFriendAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFriendUsername = itemView.findViewById(R.id.textFriendUsername);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            imageLastMessageStatus = itemView.findViewById(R.id.imageLastMessageStatus);
            imageFriendAvatar = itemView.findViewById(R.id.imageFriendAvatar);
        }
    }
}
