package edu.northeastern.guildly;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ViewHabitsActivity extends AppCompatActivity {

    private RecyclerView habitsRecyclerView;
    private List<Habit> userHabits;
    private HabitAdapter habitAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_habits);

        habitsRecyclerView = findViewById(R.id.habits_recycler_view);


        userHabits = new ArrayList<>(Arrays.asList(
                new Habit("Drink 64oz of water", R.drawable.ic_water),
                new Habit("Workout for 30 mins", R.drawable.ic_workout),
                new Habit("Do homework", R.drawable.ic_homework),
                new Habit("Read a book", R.drawable.ic_book),
                new Habit("Meditate for 10 minutes", R.drawable.ic_meditation),
                new Habit("Save money today", R.drawable.ic_savemoney),
                new Habit("Eat vegetables", R.drawable.ic_vegetable),
                new Habit("No phone after 10PM", R.drawable.ic_phonebanned)
        ));

        habitAdapter = new HabitAdapter(userHabits);
        habitsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        habitsRecyclerView.setAdapter(habitAdapter);
    }
}