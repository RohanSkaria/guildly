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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                int unreadCount = 0;
                Message lastMsg = null;
                long maxTime = -1;

                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    Message msg = msgSnap.getValue(Message.class);
                    if (msg != null) {
                        if (msg.timestamp > maxTime) {
                            maxTime = msg.timestamp;
                            lastMsg = msg;
                        }
                        if (!msg.senderId.equals(currentUserId) && "SENT".equals(msg.status)) {
                            unreadCount++;
                        }
                    }
                }

                if (lastMsg != null) {
                    holder.textLastMessage.setText(lastMsg.content);
                    holder.textTimestamp.setText(formatTimestamp(lastMsg.timestamp));
                }

                if (unreadCount > 0) {
                    holder.textUnreadCount.setVisibility(View.VISIBLE);
                    holder.textUnreadCount.setText(String.valueOf(unreadCount));
                } else {
                    holder.textUnreadCount.setVisibility(View.GONE);
                }
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

    private String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textFriendUsername, textLastMessage, textTimestamp, textUnreadCount;
        ImageView imageFriendAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFriendUsername = itemView.findViewById(R.id.textFriendUsername);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textUnreadCount = itemView.findViewById(R.id.textUnreadCount);
            imageFriendAvatar = itemView.findViewById(R.id.imageFriendAvatar);
        }
    }
}
