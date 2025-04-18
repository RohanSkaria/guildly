package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.northeastern.guildly.R;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(String userKey);
        void onReject(String userKey);
    }

    private List<String> userKeys;
    private List<String> usernames;
    private OnRequestActionListener listener;

    public FriendRequestAdapter(List<String> userKeys, List<String> usernames, OnRequestActionListener listener) {
        this.userKeys = userKeys;
        this.usernames = usernames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        String userKey = userKeys.get(position);
        String username = usernames.get(position);

        holder.textUsername.setText(username);
        holder.buttonAccept.setOnClickListener(v -> listener.onAccept(userKey));
        holder.buttonReject.setOnClickListener(v -> listener.onReject(userKey));
    }

    @Override
    public int getItemCount() {
        return userKeys.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername;
        ImageButton buttonAccept, buttonReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonReject = itemView.findViewById(R.id.buttonReject);
        }
    }
}

