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

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.FriendChatItem;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.data.User;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnFriendChatClick {
        void onChatClicked(FriendChatItem item);
    }

    private List<FriendChatItem> friendChatList;
    private OnFriendChatClick listener;
    private String currentUserId;

    public ChatListAdapter(List<FriendChatItem> friendChatList,
                           OnFriendChatClick listener) {
        this.friendChatList = friendChatList;
        this.listener = listener;
        this.currentUserId = edu.northeastern.guildly.MainActivity.currentUserEmail.replace(".", ",");
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
        holder.imageUnreadIcon.setVisibility(View.GONE);
        FriendChatItem item = friendChatList.get(position);

        holder.textFriendUsername.setText(item.friendUsername);
        holder.textLastMessage.setText(item.lastMessage);
        holder.textTimestamp.setText(item.timestamp);

        holder.imageFriendAvatar.setImageResource(R.drawable.unknown_profile);

        DatabaseReference friendRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(item.friendKey);

        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User friendUser = snapshot.getValue(User.class);
                if (friendUser != null && friendUser.profilePicUrl != null) {

                    int resourceId;
                    switch (friendUser.profilePicUrl) {
                        case "gamer":
                            resourceId = R.drawable.gamer;
                            break;
                        case "man":
                            resourceId = R.drawable.man;
                            break;
                        case "girl":
                            resourceId = R.drawable.girl;
                            break;
                        default:
                            resourceId = R.drawable.unknown_profile;
                            break;
                    }
                    holder.imageFriendAvatar.setImageResource(resourceId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(item.chatId)
                .child("messages");

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasUnread = false;
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    Message msg = msgSnap.getValue(Message.class);
                    if (msg != null && !msg.senderId.equals(currentUserId) && "SENT".equals(msg.status)) {
                        hasUnread = true;
                        break;
                    }
                }
                holder.imageUnreadIcon.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

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
        ImageView imageFriendAvatar, imageUnreadIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFriendUsername = itemView.findViewById(R.id.textFriendUsername);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            imageFriendAvatar = itemView.findViewById(R.id.imageFriendAvatar);
            imageUnreadIcon = itemView.findViewById(R.id.imageUnreadIcon);
        }
    }
}