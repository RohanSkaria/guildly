package edu.northeastern.guildly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.LeaderboardItem;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardItem> leaderboardItems;

    public LeaderboardAdapter(List<LeaderboardItem> leaderboardItems) {
        this.leaderboardItems = leaderboardItems;
    }

    public void setLeaderboardItems(List<LeaderboardItem> newItems) {
        this.leaderboardItems = newItems;
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
        holder.username.setText(item.getUsername());
        holder.profileImage.setImageResource(item.getProfileImageRes());

        // If you want to show a "dot" or something for the streak, you can do that.
        // If you want to show a numeric streak, you might place it in a separate TextView, or
        // interpret the progress bar in some manner.
        // Let's say the progress bar is scaled to a max of 100 for demonstration:

        int maxStreakToShow = 30; // just an example "cap"
        holder.streakProgress.setMax(maxStreakToShow);
        holder.streakProgress.setProgress(Math.min(item.getStreakCount(), maxStreakToShow));
    }

    @Override
    public int getItemCount() {
        return leaderboardItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username;
        ProgressBar streakProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.username);
            streakProgress = itemView.findViewById(R.id.streak_progress_bar);
        }
    }
}
