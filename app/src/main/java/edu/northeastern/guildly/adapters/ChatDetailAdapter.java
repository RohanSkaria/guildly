package edu.northeastern.guildly.adapters;

import android.annotation.SuppressLint;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.Message;
import edu.northeastern.guildly.data.User;

public class ChatDetailAdapter extends RecyclerView.Adapter<ChatDetailAdapter.ViewHolder> {

    private static final int VIEW_TYPE_MINE = 1;

    private static final int VIEW_TYPE_THEIR = 2;

    private List<Message> messages;
    private String myUserKey;

    public ChatDetailAdapter(List<Message> messages, String myUserKey) {
        this.messages = messages;
        this.myUserKey = myUserKey;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.senderId.equals(myUserKey)) {
            return VIEW_TYPE_MINE;
        } else {
            return VIEW_TYPE_THEIR;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_MINE)
                ? R.layout.item_chat_mine
                : R.layout.item_chat_their;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);

        holder.textMessage.setText(msg.content);


        String timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(msg.timestamp));
        holder.textTime.setText(timeText);


        // I set 1s to show the difference for other's message(although I think it's unnecessary)
        if (msg.senderId.equals(myUserKey)) {
            // my message
            if ("READ".equals(msg.status)) {
                holder.imageStatus.setImageResource(R.drawable.ic_eye);     // sent and opened
            } else {
                holder.imageStatus.setImageResource(R.drawable.ic_check);   // sent and not opened
            }
        } else {
            // other's message
            if ("READ".equals(msg.status)) {
                holder.imageStatus.setImageResource(R.drawable.ic_msg_hollow); // received and opened
                holder.imageStatus.setVisibility(View.VISIBLE);
            } else {
                holder.imageStatus.setImageResource(R.drawable.ic_msg_solid);  // received and not opened
                holder.imageStatus.setVisibility(View.VISIBLE);
            }
        }


        if (getItemViewType(position) == VIEW_TYPE_THEIR && holder.senderAvatar != null) {

            DatabaseReference senderRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(msg.senderId);

            senderRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User senderUser = snapshot.getValue(User.class);
                    if (senderUser != null && senderUser.profilePicUrl != null) {

                        int resourceId;
                        switch (senderUser.profilePicUrl) {
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
                        holder.senderAvatar.setImageResource(resourceId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imageStatus;
        CircleImageView senderAvatar;

        ViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            imageStatus = itemView.findViewById(R.id.imageStatus);


            if (itemView.findViewById(R.id.senderAvatar) != null) {
                senderAvatar = itemView.findViewById(R.id.senderAvatar);
            }
        }
    }

}
