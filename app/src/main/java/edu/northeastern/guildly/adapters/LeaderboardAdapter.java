package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.LeaderboardItem;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardItem> leaderboardItems;
    private List<Integer> ranks;

    public LeaderboardAdapter(List<LeaderboardItem> leaderboardItems) {
        this.leaderboardItems = leaderboardItems;
        this.ranks = calculateRanks(leaderboardItems);
    }

    private List<Integer> calculateRanks(List<LeaderboardItem> items) {
        List<Integer> result = new ArrayList<>();
        int currentRank = 1;

        for (int i = 0; i < items.size(); i++) {
            if (i > 0 && items.get(i).getStreakCount() != items.get(i - 1).getStreakCount()) {
                currentRank = i + 1;
            }
            result.add(currentRank);
        }

        return result;
    }

    public void setLeaderboardItems(List<LeaderboardItem> newItems) {
        this.leaderboardItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeaderboardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate your existing item_leaderboard.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardAdapter.ViewHolder holder, int position) {

        LeaderboardItem item = leaderboardItems.get(position);
//        holder.rank.setText(String.valueOf(position + 1));
        holder.rank.setText(String.valueOf(ranks.get(position)));
        holder.username.setText(item.getUsername());
        holder.profileImage.setImageResource(item.getProfileImageRes());
        holder.streakCountAmount.setText(String.valueOf( "Streak: " + item.getStreakCount()));


//        int maxStreakToShow = 30; // just an example "cap"
//        holder.streakProgress.setMax(maxStreakToShow);
//        holder.streakProgress.setProgress(Math.min(item.getStreakCount(), maxStreakToShow));
    }

    @Override
    public int getItemCount() {
        return leaderboardItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username;
//        ProgressBar streakProgress;
        TextView streakCountAmount;
        TextView rank;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.username);
//            streakProgress = itemView.findViewById(R.id.streak_progress_bar);
            streakCountAmount = itemView.findViewById(R.id.friends_leaderboard_streak_text);
            rank = itemView.findViewById(R.id.rank);
        }
    }
}
