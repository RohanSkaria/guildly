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
import edu.northeastern.guildly.data.Habit;

public class AllHabitsAdapter extends RecyclerView.Adapter<AllHabitsAdapter.AllHabitsViewHolder> {

    private final List<Habit> allHabits; // already sorted by streak desc

    public AllHabitsAdapter(List<Habit> allHabits) {
        this.allHabits = allHabits;
    }

    @NonNull
    @Override
    public AllHabitsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_all_habits, parent, false);
        return new AllHabitsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AllHabitsViewHolder holder, int position) {
        Habit h = allHabits.get(position);
        holder.bind(h);
    }

    @Override
    public int getItemCount() {
        return allHabits.size();
    }

    static class AllHabitsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvStreak;

        public AllHabitsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon   = itemView.findViewById(R.id.ivHabitIcon);
            tvName   = itemView.findViewById(R.id.tvHabitName);
            tvStreak = itemView.findViewById(R.id.tvHabitStreak);
        }

        void bind(Habit h) {
            ivIcon.setImageResource(h.getIconResId());
            tvName.setText(h.getHabitName());
            tvStreak.setText("🔥 " + h.getStreakCount() + " days");
        }
    }
}
