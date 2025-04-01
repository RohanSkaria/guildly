package edu.northeastern.guildly.signUpFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.northeastern.guildly.Habit;
import edu.northeastern.guildly.R;

public class HabitSelectionFragment extends Fragment {
    private List<CheckBox> habitCheckboxes = new ArrayList<>();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit_selection, container, false);

        LinearLayout habitContainer = view.findViewById(R.id.habitContainer);

        // Get previously selected habits
        ArrayList<String> selectedHabits = null;
        if (getArguments() != null) {
            selectedHabits = getArguments().getStringArrayList("selectedHabits");
        }

        // Create checkboxes for each habit
        for (Habit habit : predefinedHabits) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(habit.getName());
            checkBox.setTag(habit.getName());

            // Check if this habit was previously selected
            if (selectedHabits != null && selectedHabits.contains(habit.getName())) {
                checkBox.setChecked(true);
            }

            habitContainer.addView(checkBox);
            habitCheckboxes.add(checkBox);
        }

        return view;
    }

    public boolean validateAndSaveData(Bundle data) {
        ArrayList<String> selectedHabits = new ArrayList<>();

        for (CheckBox checkBox : habitCheckboxes) {
            if (checkBox.isChecked()) {
                selectedHabits.add(checkBox.getTag().toString());
            }
        }

        data.putStringArrayList("selectedHabits", selectedHabits);
        return true; // Habits are optional, so always valid
    }
}