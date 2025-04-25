package edu.northeastern.guildly.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.Habit;

/**
 * ONE unified adapter that handles either:
 *  - "Selection mode" (isSelectionMode=true): user picks isTracked or not
 *  - "Home mode" (isSelectionMode=false): daily completion logic
 */
public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.ViewHolder> {
    private static final String TAG = "HabitAdapter";

    private final List<Habit> habitList;
    private final DatabaseReference userHabitsRef;
    private final boolean isSelectionMode;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HabitAdapter(List<Habit> habitList,
                        DatabaseReference userHabitsRef,
                        boolean isSelectionMode) {
        this.habitList = habitList;
        this.userHabitsRef = userHabitsRef;
        this.isSelectionMode = isSelectionMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= habitList.size()) {
            Log.e(TAG, "Invalid position: " + position + ", list size: " + habitList.size());
            return;
        }

        Habit habit = habitList.get(position);
        if (habit == null) {
            Log.e(TAG, "Null habit at position: " + position);
            return;
        }

        holder.bind(habit);
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView habitImage;
        TextView habitName;
        TextView habitStreak;
        CheckBox habitCheckBox;
        TextView lockMessage;
        LinearLayout itemHabit;
        Context context;

        ViewHolder(View itemView) {
            super(itemView);
            habitImage   = itemView.findViewById(R.id.habit_image);
            habitName    = itemView.findViewById(R.id.habit_name);
            habitStreak  = itemView.findViewById(R.id.habit_streak);
            habitCheckBox= itemView.findViewById(R.id.habit_item);
            lockMessage  = itemView.findViewById(R.id.lockMessage);
            itemHabit    = itemView.findViewById(R.id.item_habit);
            context      = itemView.getContext();
        }

        @SuppressLint("SetTextI18n")
        void bind(Habit habit) {
            try {
                // Icon and name
                habitImage.setImageResource(habit.getIconResId());
                String name = habit.getHabitName() != null ? habit.getHabitName() : "Unnamed Habit";
                habit.setHabitName(name);
                habitName.setText(name);

                // Default border
                itemHabit.setBackgroundResource(R.drawable.habit_item_border);
                habitStreak.setVisibility(View.VISIBLE);
                lockMessage.setVisibility(View.GONE);

                if (isSelectionMode) {
                    // Selection: track or untrack
                    habitStreak.setText("Streak: " + habit.getStreakCount());
                    habitCheckBox.setVisibility(View.VISIBLE);

                    // Clear old listener
                    habitCheckBox.setOnCheckedChangeListener(null);
                    habitCheckBox.setChecked(habit.isTracked());
                    habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        habit.setTracked(isChecked);
                        if (userHabitsRef != null) {
                            String safeName = habit.getHabitName().replace(".", "_");
                            userHabitsRef.child(safeName)
                                    .child("tracked").setValue(isChecked)
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating tracked: " + habit.getHabitName(), e));
                        }
                    });
                } else {
                    // Home: daily completion
                    long now = System.currentTimeMillis();
                    habitStreak.setText("Streak: " + habit.getStreakCount());
                    habitCheckBox.setVisibility(View.VISIBLE);

                    // Clear any previous listener
                    habitCheckBox.setOnCheckedChangeListener(null);

                    boolean locked = now < habit.getNextAvailableTime();
                    if (locked) {
                        habitCheckBox.setChecked(true);
                        habitCheckBox.setEnabled(false);
                        itemHabit.setBackground(ContextCompat.getDrawable(context, R.drawable.habit_item_border_tint));
                    } else {
                        habitCheckBox.setChecked(false);
                        habitCheckBox.setEnabled(true);
                        habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked && !habit.isCompletedToday()) {
                                handleCompletion(habit);
                            } else if (!isChecked) {
                                // Prevent unchecking
                                habitCheckBox.setChecked(true);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding habit: " + habit.getHabitName(), e);
                habitName.setText((habit.getHabitName() != null ? habit.getHabitName() : "Unknown") + " (Error)");
                habitImage.setImageResource(R.drawable.unknown_profile);
            }
        }

        private void handleCompletion(Habit habit) {
            if (habit == null) return;
            try {
                long now = System.currentTimeMillis();
                long oneDay = 24L * 60L * 60L * 1000L;
                long diff = habit.getLastCompletedTime() > 0 ? now - habit.getLastCompletedTime() : 0;

                if (habit.getLastCompletedTime() == 0) {
                    habit.setStreakCount(1);
                } else if (diff < (oneDay * 2)) {
                    habit.setStreakCount(habit.getStreakCount() + 1);
                } else {
                    habit.setStreakCount(1);
                }

                habit.setLastCompletedTime(now);
                habit.setCompletedToday(true);
                habit.setNextAvailableTime(now + oneDay);

                // Update UI
                habitStreak.setText("Streak: " + habit.getStreakCount());
                habitCheckBox.setEnabled(false);
                itemHabit.setBackground(ContextCompat.getDrawable(context, R.drawable.habit_item_border_tint));

                mainHandler.post(() -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
                });

                // Persist to Firebase
                if (userHabitsRef != null) {
                    String safeName = habit.getHabitName().replace(".", "_");
                    Map<String,Object> updates = new HashMap<>();
                    updates.put("tracked", habit.isTracked());
                    updates.put("streakCount", habit.getStreakCount());
                    updates.put("lastCompletedTime", habit.getLastCompletedTime());
                    updates.put("completedToday", habit.isCompletedToday());
                    updates.put("nextAvailableTime", habit.getNextAvailableTime());

                    userHabitsRef.child(safeName)
                            .updateChildren(updates)
                            .addOnFailureListener(e -> Log.e(TAG, "Failed update habit: " + e.getMessage()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in handleCompletion: " + e.getMessage(), e);
                Toast.makeText(context, "Error updating habit progress", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
