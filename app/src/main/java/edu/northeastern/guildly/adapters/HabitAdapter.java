package edu.northeastern.guildly.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
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
    private final DatabaseReference userHabitsRef; // optional for partial updates
    private final boolean isSelectionMode;         // if true => sign-up selection

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
            itemHabit = itemView.findViewById(R.id.item_habit);
            context = itemView.getContext();
        }

        @SuppressLint("SetTextI18n")
        void bind(Habit habit) {
            try {
                habitImage.setImageResource(habit.getIconResId());
                habitName.setText(habit.getHabitName());

                // set background
                itemHabit.setBackgroundResource(R.drawable.habit_item_border);

                // If "Selection mode," the CheckBox means "isTracked"
                // If "Home mode," the CheckBox means daily completion for "completedToday"
                if (isSelectionMode) {
                    // Hide the streak text & lockMessage if you want
                    habitStreak.setVisibility(View.GONE);
                    lockMessage.setVisibility(View.GONE);

                    // Show the checkBox to set "isTracked"
                    habitCheckBox.setVisibility(View.VISIBLE);
                    habitCheckBox.setOnCheckedChangeListener(null);
                    habitCheckBox.setChecked(habit.isTracked());
                    habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        habit.setTracked(isChecked);
                        // If we have a DB ref, do partial update
                        if (userHabitsRef != null) {
                            String safeHabitName = habit.getHabitName().replace(".", "_");
                            userHabitsRef.child(safeHabitName)
                                    .child("tracked").setValue(isChecked)
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error updating tracked state for: " +
                                                    habit.getHabitName(), e));
                        }
                    });
                } else {
                    // HOME MODE: daily completion logic
                    // If user is still locked out
                    long now = System.currentTimeMillis();
                    if (now >= habit.getNextAvailableTime()) {
                        // user can complete again
                        habit.setCompletedToday(false);
                    }

                    // Show streak
                    habitStreak.setVisibility(View.VISIBLE);
                    habitStreak.setText("Streak: " + habit.getStreakCount());

                    if (now < habit.getNextAvailableTime()) {
                        habitCheckBox.setVisibility(View.VISIBLE);
                        habitCheckBox.setChecked(true);
                        habitCheckBox.setEnabled(false);
                        lockMessage.setVisibility(View.GONE);
                        itemHabit.setBackground(ContextCompat.getDrawable(context, R.drawable.habit_item_border_tint));
                    } else {
                        habitCheckBox.setVisibility(View.VISIBLE);
                        habitCheckBox.setEnabled(true);
                        habitCheckBox.setChecked(false);
                        lockMessage.setVisibility(View.GONE);

                        habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                if (!habit.isCompletedToday()) {
                                    handleCompletion(habit);
                                }
                            } else {
                                habitCheckBox.setChecked(true); // disallow uncheck
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding habit: " + habit.getHabitName(), e);

                // Try to recover by showing minimal UI
                if (habitName != null) {
                    habitName.setText(habit.getHabitName() + " (Error)");
                }
                if (habitImage != null) {
                    habitImage.setImageResource(R.drawable.unknown_profile);
                }
            }
        }

        private void handleCompletion(Habit habit) {
            if (habit == null) {
                Log.e(TAG, "Attempted to complete a null habit");
                return;
            }

            try {
                long now = System.currentTimeMillis();
                long oneDay = 24L * 60L * 60L * 1000L;

                long diff = 0;
                if (habit.getLastCompletedTime() > 0) {
                    diff = now - habit.getLastCompletedTime();
                }

                if (habit.getLastCompletedTime() == 0) {
                    // First completion
                    habit.setStreakCount(1);
                } else if (diff < (oneDay * 2)) {
                    // Continuing streak
                    habit.setStreakCount(habit.getStreakCount() + 1);
                } else {
                    // Streak broken
                    habit.setStreakCount(1);
                }

                habit.setLastCompletedTime(now);
                habit.setCompletedToday(true);
                habit.setNextAvailableTime(now + oneDay);

                // Update UI
                notifyDataSetChanged();

                // Update Firebase
                if (userHabitsRef != null) {
                    String safeHabitName = habit.getHabitName().replace(".", "_");
                    Map<String,Object> updates = new HashMap<>();
                    updates.put("tracked", habit.isTracked());
                    updates.put("streakCount", habit.getStreakCount());
                    updates.put("lastCompletedTime", habit.getLastCompletedTime());
                    updates.put("completedToday", habit.isCompletedToday());
                    updates.put("nextAvailableTime", habit.getNextAvailableTime());

                    userHabitsRef.child(safeHabitName).updateChildren(updates)
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update habit: " + e.getMessage());
                            });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in handleCompletion: " + e.getMessage());
                if (context != null) {
                    Toast.makeText(context, "Error updating habit progress", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}