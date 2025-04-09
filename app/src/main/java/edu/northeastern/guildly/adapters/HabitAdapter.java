package edu.northeastern.guildly.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        // We can reuse the same layout if we want "item_habit"
        // or create separate. We'll assume "item_habit"
        // has an ImageView, a name, a separate "CheckBox" for signUp
        // or "completedToday" usage. We'll handle mode logic in bind.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Habit habit = habitList.get(position);
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
                habitCheckBox.setOnCheckedChangeListener(null);
                habitCheckBox.setChecked(habit.isTracked());
                habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    habit.setTracked(isChecked);
                    // If we have a DB ref, do partial update
                    if (userHabitsRef != null) {
                        userHabitsRef.child(habit.getHabitName())
                                .child("tracked").setValue(isChecked);
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


                if (habit.isCompletedToday()) {
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

//                // Decide if locked or available
//                if (now < habit.getNextAvailableTime()) {
//                    // LOCKED
//                    habitCheckBox.setVisibility(View.GONE);
//                    lockMessage.setVisibility(View.VISIBLE);
//                } else {
//                    // AVAILABLE => show the "completedToday" checkbox
//                    habitCheckBox.setVisibility(View.VISIBLE);
//                    lockMessage.setVisibility(View.GONE);
//
//                    habitCheckBox.setOnCheckedChangeListener(null);
//                    habitCheckBox.setChecked(habit.isCompletedToday());
//
//                    habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                        if (isChecked) {
//                            if (!habit.isCompletedToday()) {
//                                handleCompletion(habit);
//                            }
//                        } else {
//                            // no uncheck after done
//                            habitCheckBox.setChecked(true);
//                        }
//                    });
//                }
            }
        }

        /**
         * Mark the habit as completed for the day, set streak & 24-hr lock
         */
        private void handleCompletion(Habit habit) {
            long now = System.currentTimeMillis();
            long oneDay = 24L * 60L * 60L * 1000L;
            long diff   = now - habit.getLastCompletedTime();

            if (habit.getLastCompletedTime() == 0) {
                habit.setStreakCount(1);
            } else if (diff < (oneDay * 2)) {
                // ~48 hours => increment
                habit.setStreakCount(habit.getStreakCount() + 1);
            } else {
                // reset
                habit.setStreakCount(1);
            }

            habit.setLastCompletedTime(now);
            habit.setCompletedToday(true);
            habit.setNextAvailableTime(now + oneDay);

            // Refresh row
            notifyDataSetChanged();

            // Partial update
            if (userHabitsRef != null) {
                Map<String,Object> updates = new HashMap<>();
                updates.put("tracked", habit.isTracked());
                updates.put("streakCount", habit.getStreakCount());
                updates.put("lastCompletedTime", habit.getLastCompletedTime());
                updates.put("completedToday", habit.isCompletedToday());
                updates.put("nextAvailableTime", habit.getNextAvailableTime());

                userHabitsRef.child(habit.getHabitName()).updateChildren(updates);
            }
        }
    }
}
