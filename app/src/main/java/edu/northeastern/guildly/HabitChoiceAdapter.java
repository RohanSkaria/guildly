package edu.northeastern.guildly;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class HabitChoiceAdapter extends ArrayAdapter<Habit> {
    public HabitChoiceAdapter(@NonNull Context context, @NonNull List<Habit> habits) {
        super(context, 0, habits);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_habit_choice, parent, false);

        Habit habit = getItem(position);
        ((TextView) convertView.findViewById(R.id.name)).setText(habit.getName());
        ((ImageView) convertView.findViewById(R.id.icon)).setImageResource(habit.getIconResId());

        return convertView;
    }
}
