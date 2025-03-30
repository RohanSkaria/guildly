package edu.northeastern.guildly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {
    private TextView UserName;
    private Button btnAddHabit;
    private RecyclerView HabitList;
    private RecyclerView leaderboardRecyclerView;
    private List<Habit> habitList;
    private List<Friend> friendsList;
    private HabitAdapter habitAdapter;
    private LeaderboardAdapter leaderboardAdapter;

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

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        UserName = view.findViewById(R.id.user_name);
        btnAddHabit = view.findViewById(R.id.btn_add_habit);
        HabitList = view.findViewById(R.id.habit_list);
        leaderboardRecyclerView = view.findViewById(R.id.leaderboard);

        habitList = new ArrayList<>();
        habitList.add(new Habit("Drink 64oz of water", R.drawable.ic_water));
        habitList.add(new Habit("Workout for 30 mins", R.drawable.ic_workout));

        habitAdapter = new HabitAdapter(habitList);
        HabitList.setLayoutManager(new LinearLayoutManager(getContext()));
        HabitList.setAdapter(habitAdapter);

        btnAddHabit.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_habit, null);
            ListView listView = dialogView.findViewById(R.id.habit_list_view);

            HabitChoiceAdapter adapter = new HabitChoiceAdapter(getContext(), predefinedHabits);
            listView.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("Choose a Habit")
                    .setView(dialogView)
                    .create();

            listView.setOnItemClickListener((parent, itemView, position, id) -> {
                Habit selected = predefinedHabits.get(position);
                habitList.add(selected);
                habitAdapter.notifyDataSetChanged();
                dialog.dismiss();
            });

            dialog.show();
        });

        initializeLeaderboard();

        return view;
    }

    private void initializeLeaderboard() {
        friendsList = new ArrayList<>();

        friendsList.add(new Friend("RohanS3", 90, R.drawable.gamer));
        friendsList.add(new Friend("ParwazS98", 70, R.drawable.man));
        friendsList.add(new Friend("PMadisen43", 50, R.drawable.girl));

        Collections.sort(friendsList, new Comparator<Friend>() {
            @Override
            public int compare(Friend f1, Friend f2) {
                return Integer.compare(f2.getStreakCount(), f1.getStreakCount());
            }
        });

        leaderboardAdapter = new LeaderboardAdapter(friendsList);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        leaderboardRecyclerView.setAdapter(leaderboardAdapter);
    }
}
