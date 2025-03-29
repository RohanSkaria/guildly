package edu.northeastern.guildly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView UserName;
    private Button btnAddHabit;
    private RecyclerView HabitList;
    private List<Habit> habitList;
    private HabitAdapter habitAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        UserName = view.findViewById(R.id.user_name);
        btnAddHabit = view.findViewById(R.id.btn_add_habit);
        HabitList = view.findViewById(R.id.habit_list);

        // Initialize Habit List
        habitList = new ArrayList<>();
        habitList.add(new Habit("Drink 64oz of water"));
        habitList.add(new Habit("Workout for 30 mins"));

        habitAdapter = new HabitAdapter(habitList);
        HabitList.setLayoutManager(new LinearLayoutManager(getContext()));
        HabitList.setAdapter(habitAdapter);

        btnAddHabit.setOnClickListener(v -> {
            habitList.add(new Habit("New Habit"));
            habitAdapter.notifyItemInserted(habitList.size() - 1);
        });

        return view;
    }
}
