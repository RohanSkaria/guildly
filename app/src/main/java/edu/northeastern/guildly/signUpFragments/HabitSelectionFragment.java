package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.northeastern.guildly.R;
import edu.northeastern.guildly.data.Habit;

/**
 * A sign-up fragment that uses a simple LinearLayout with CheckBoxes
 * to let the user pick which habits to track (isTracked = true).
 * We'll store them individually in Realtime DB under /users/<userId>/habits/<habitName>,
 * but always keep all 8 with the correct isTracked boolean.
 */
public class HabitSelectionFragment extends Fragment {

    private static final String TAG = "HabitSelectionFragment";

    private LinearLayout habitContainer;
    private List<CheckBox> habitCheckboxes = new ArrayList<>();

    // The 8 possible habits for sign-up
    private List<Habit> predefinedHabits = Arrays.asList(
            new Habit("Drink 64oz of water", R.drawable.ic_water),
            new Habit("Workout for 30 mins", R.drawable.ic_workout),
            new Habit("Do homework", R.drawable.ic_homework),
            new Habit("Read a book", R.drawable.ic_book),
            new Habit("Meditate for 10 minutes", R.drawable.ic_meditation),
            new Habit("Save money today", R.drawable.ic_savemoney),
            new Habit("Eat vegetables", R.drawable.ic_vegetable),
            new Habit("No phone after 10PM", R.drawable.ic_phonebanned)
    );

    public HabitSelectionFragment() {
        // Required empty public constructor
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_habit_selection, container, false);
        habitContainer = view.findViewById(R.id.habitContainer);


        habitContainer.removeAllViews();


        for (Habit predefined : predefinedHabits) {

            View habitItemView = inflater.inflate(R.layout.item_predefined_habit, habitContainer, false);

            ImageView habitIcon = habitItemView.findViewById(R.id.predef_habit_icon);
            TextView habitName = habitItemView.findViewById(R.id.predef_habit_name);
            CheckBox habitCheck = habitItemView.findViewById(R.id.predef_habit_check);


            habitIcon.setImageResource(predefined.getIconResId());
            habitName.setText(predefined.getHabitName());


            habitCheck.setTag(predefined);


            habitCheckboxes.add(habitCheck);


            habitContainer.addView(habitItemView);
        }

        return view;
    }

    /**
     * Called when the user finishes the sign-up step for habit selection.
     * We now store all 8 habits with isTracked = true/false as indicated by the CheckBox.
     */
    /**
     * Called when the user finishes the sign-up step for habit selection.
     * We now store all 8 habits with isTracked = true/false as indicated by the CheckBox.
     */
    public boolean validateAndSaveData(Bundle data) {
        //  Identify which user we are storing for
        String userId = data.getString("userId");
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "No userId in data. Cannot save habits to DB.");
            // We won't fail the flow, but obviously habits won't be stored
            return true; // still "valid" from a UI perspective
        }

        // Reference to /users/<userId>/habits
        DatabaseReference userHabitsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("habits");

        // Create a list to track selected habits for review screen
        ArrayList<String> selectedHabits = new ArrayList<>();

        // For each CheckBox => set isTracked accordingly, store to DB
        for (CheckBox cb : habitCheckboxes) {
            Habit h = (Habit) cb.getTag();
            boolean tracked = cb.isChecked();
            h.setTracked(tracked);

            // Track selected habits for review screen
            if (tracked) {
                selectedHabits.add(h.getHabitName());
            }

            // Use sanitized habit name as key to avoid Firebase path issues
            String safeHabitName = h.getHabitName().replace(".", "_");

            // Store each property individually instead of the whole object
            userHabitsRef.child(safeHabitName).child("habitName").setValue(h.getHabitName());
            userHabitsRef.child(safeHabitName).child("iconResId").setValue(h.getIconResId());
            userHabitsRef.child(safeHabitName).child("tracked").setValue(tracked);
            userHabitsRef.child(safeHabitName).child("streakCount").setValue(h.getStreakCount());
            userHabitsRef.child(safeHabitName).child("lastCompletedTime").setValue(h.getLastCompletedTime());
            userHabitsRef.child(safeHabitName).child("completedToday").setValue(h.isCompletedToday());
            userHabitsRef.child(safeHabitName).child("nextAvailableTime").setValue(h.getNextAvailableTime());
        }

        // Add the selected habits to the bundle for the review screen
        data.putStringArrayList("selectedHabits", selectedHabits);

        Log.d(TAG, "Finished writing all 8 habits to DB with correct isTracked flags.");
        return true;
    }
}