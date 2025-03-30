package edu.northeastern.guildly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habitList;
    public HabitAdapter(List<Habit> habitList) {
        this.habitList = habitList;
    }

    @NonNull
    @Override
    public HabitAdapter.HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitAdapter.HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.habit_name.setText(habit.getName());
        // need to set checkbox state here

    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        CheckBox habit_checkbox;
        TextView habit_name;

        public HabitViewHolder(@NonNull View itemVeiw) {
            super(itemVeiw);
            habit_name = itemVeiw.findViewById(R.id.habit_name);
            habit_checkbox= itemVeiw.findViewById(R.id.habit_item);
        }
    }
}
