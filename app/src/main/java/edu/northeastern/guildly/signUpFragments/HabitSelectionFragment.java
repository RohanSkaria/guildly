package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

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

        // Dynamically create checkboxes for each predefined habit
        for (Habit predefined : predefinedHabits) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(predefined.getHabitName());
            // Store the entire Habit object in the tag
            checkBox.setTag(predefined);
            habitContainer.addView(checkBox);
            habitCheckboxes.add(checkBox);
        }

        return view;
    }

    /**
     * Called when the user finishes the sign-up step for habit selection.
     * We now store all 8 habits with isTracked = true/false as indicated by the CheckBox.
     */
    public boolean validateAndSaveData(Bundle data) {
        // 1) Identify which user we are storing for
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

        // 2) For each CheckBox => set isTracked accordingly, store to DB
        for (CheckBox cb : habitCheckboxes) {
            Habit h = (Habit) cb.getTag();
            boolean tracked = cb.isChecked();
            h.setTracked(tracked);
            userHabitsRef.child(h.getHabitName()).setValue(h);
        }

        Log.d(TAG, "Finished writing all 8 habits to DB with correct isTracked flags.");
        return true;
    }
}
