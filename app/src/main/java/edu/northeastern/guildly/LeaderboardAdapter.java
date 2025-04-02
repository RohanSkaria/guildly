package edu.northeastern.guildly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.northeastern.guildly.data.Friend;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private List<Friend> friendsList;
    private int maxStreak;

    public LeaderboardAdapter(List<Friend> friendsList) {
        this.friendsList = friendsList;


        this.maxStreak = 0;
        for (Friend friend : friendsList) {
            if (friend.getStreakCount() > maxStreak) {
                maxStreak = friend.getStreakCount();
            }
        }
        if (maxStreak == 0) maxStreak = 1;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        Friend friend = friendsList.get(position);
        holder.username.setText(friend.getUsername());
        holder.profileImage.setImageResource(friend.getProfileImageResource());

        int progressValue = (int)(((float)friend.getStreakCount() / maxStreak) * 100);
        holder.streakProgressBar.setProgress(progressValue);


        holder.streakDot.setVisibility(friend.getStreakCount() < maxStreak ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username;
        ProgressBar streakProgressBar;
        TextView streakDot;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.username);
            streakProgressBar = itemView.findViewById(R.id.streak_progress_bar);
            streakDot = itemView.findViewById(R.id.streak_dot);
        }
    }
}