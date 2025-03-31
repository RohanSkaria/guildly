package edu.northeastern.guildly.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.Message;

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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_MINE)
                ? R.layout.item_chat_mine
                : R.layout.item_chat_their;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message msg = messages.get(position);

        holder.textMessage.setText(msg.content);

        // Format time
        String timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(msg.timestamp));
        holder.textTime.setText(timeText);

        // Set status icon
        switch (msg.status) {
            case "SENT":
                holder.imageStatus.setImageResource(R.drawable.ic_msg_solid);
                break;
            case "READ":
                holder.imageStatus.setImageResource(R.drawable.ic_msg_hollow);
                break;
            default:
                holder.imageStatus.setImageResource(R.drawable.ic_msg_solid);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imageStatus;

        ViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            imageStatus = itemView.findViewById(R.id.imageStatus);
        }
    }
}
