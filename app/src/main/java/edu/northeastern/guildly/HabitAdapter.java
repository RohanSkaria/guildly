package edu.northeastern.guildly;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    @NonNull
    @Override
    public HabitAdapter.HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull HabitAdapter.HabitViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        CheckBox habit_checkbox;

        public HabitViewHolder(@NonNull View itemVeiw) {
            super(itemVeiw);
            habit_checkbox= itemVeiw.findViewById(R.id.habit_item);
        }
    }
}